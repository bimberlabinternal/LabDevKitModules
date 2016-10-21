/*
 * Copyright (c) 2010-2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * @cfg defaultTab The default top-level tab that will be select on load, if no more specific report is active
 * @cfg defaultReport The default report (2nd tier tab) to show on load.
 * @cfg filterTypes
 * @cfg autoLoadDefaultTab If true, the default tab will automatically load unless another is selected
 * @cfg reportNamespace The namespace where JS reports are located.  If null, it assumes none (i.e. this)
 * @cfg maxSubjectsToShow The maximum number of subject IDs to show as buttons before collapsing into a summary
 * @cfg reports
 */
Ext4.define('LDK.panel.TabbedReportPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.ldk-tabbedreportpanel',
    allowEditing: true,
    showDiscvrLink: true,
    subjectColumns: 8,
    subjectMaxRows: 9,
    rowHeight: 22,

    btnPanelPrefix: 'btnPanel',
    totalPanelPrefix: 'totalPanel',
    btnPrefix: 'btn',
    btnTypes: {
        subjects: 'Subjects',
        aliases: 'Aliases',
        conflicted: 'Conflicted',
        notfound: 'NotFound'
    },

    totalMessages: {},
    subjects: {},
    tooltips: {},

    initComponent: function(){
        Ext4.apply(this, {
            tabsReady: false,
            border: false,
            bodyStyle: 'background-color : transparent;margin-bottom: 10px;',
            reportMap: {},
            defaults: {
                border: false
            },
            items: [{
                layout: 'hbox',
                defaults: {
                    border: false
                },
                items: [{
                    xtype: 'panel',
                    defaults: {
                        border: false
                    },
                    items: [{
                        xtype: 'panel',
                        defaults: {
                            border: false
                        },
                        itemId: 'togglePanel',
                        style: 'margin-bottom:20px;',
                        layout: 'hbox',
                        items: this.getFilterOptionsItems()
                    },{
                        xtype: 'panel',
                        defaults: {
                            border: false
                        },
                        itemId: 'filterPanel',
                        layout: 'hbox'
                    }]
                },{
                    itemId: 'idPanel',
                    border: false,
                    defaults: {
                        border: false
                    }
                }]
            },{
                xtype: 'button',
                border: true,
                text: 'Update Report',
                handler: this.onSubmit,
                forceRefresh: true,
                itemId: 'submitBtn',
                disabled: true,
                scope: this,
                style:'margin-left:200px;margin-top: 10px;'
            },{
                xtype: 'container',
                items: this.getIEWarning()
            },{
                tag: 'span',
                style: 'padding: 10px'
            },{
                xtype: 'tabpanel',
                itemId: 'tabPanel',
                listeners: {
                    scope: this,
                    tabchange: this.onCategoryTabChange,
                    afterrender: function (panel) {
                        panel.getTabBar().addCls("category-tab-bar");
                    }
                }
            },{
                hidden: !this.showDiscvrLink,
                style: 'padding: 5px;padding-top: 0px;text-align: center',
                html: 'Powered By DISCVR.  <a href="https://github.com/bbimber/discvr/wiki">Click here to learn more.</a>'
            }]
        });

        if(!Ext4.isDefined(this.maxSubjectsToShow))
            this.maxSubjectsToShow = this.subjectColumns * this.subjectMaxRows;

        this.totalMessages[this.btnTypes.subjects] = "IDs found";
        this.totalMessages[this.btnTypes.aliases] = "IDs resolved from alias";
        this.totalMessages[this.btnTypes.conflicted] = "Alias conflicts";
        this.totalMessages[this.btnTypes.notfound] = "IDs not found";

        this.subjects[this.btnTypes.subjects] = [];
        this.subjects[this.btnTypes.aliases] = [];
        this.subjects[this.btnTypes.conflicted] = [];
        this.subjects[this.btnTypes.notfound] = [];

        Ext.QuickTips.init();

        this.callParent(arguments);

        this.on('afterrender', this.onAfterRender);
    },

    getDistinctCategories: function(){
        var categories = [];
        Ext4.each(this.reports, function(r){
            if (r.category){
                categories.push(r.category);
            }
        }, this);

        categories = Ext4.unique(categories);

        return categories
    },

    getIEWarning: function(){
        var toAdd = [];

        if (Ext4.isIE){
            toAdd.push({
                border: false,
                html: '<span>NOTE: You are currently using Internet Explorer.  While this page will work on any browser, it may perform better in other browsers, such as Chrome or Firefox.  For the best experience, we recommend using one of these browsers.</span>',
                style: 'padding-top: 20px;'
            });
        }

        return toAdd;
    },

    onAfterRender: function(panel){
        this.originalWidth = this.getWidth();
    },

    onCategoryTabChange: function(panel, tab, oldTab){
        //when we shift top-level tabs, it is possible for a previously loaded report to still show, yet
        //not have the right IDs.  therefore when we change tabs, we force the child to fire tabchange.
        //its listener should only reload if necessary

        //NOTE: if we have a default tab set, it will initially be active.  if we loaded the page with a different
        //top-level tab selected toggling tabs could result in loading that child tab unintentionally.
        //if we toggle to a new top-level tab, but there is no previously loaded child, treat it like no tab is selected
        var childTab = tab.getActiveTab();
        if (oldTab && childTab && !childTab.hasLoaded){
            tab.setActiveTab(null);
            childTab = null;
        }

        if (childTab){
            tab.fireEvent('tabchange', tab, childTab);
        }
    },

    setSubjMsg: function(msg){
        var target = this.down('#idPanel');
        target.removeAll();

        target.add({
            itemId: 'msgPanel',
            html: msg
        });
    },

    // Delete button and subject
    buttonHandler: function (button) {
        var section = button.section;
        var panel = button.up('#' + this.btnPanelPrefix + section);

        var index = this.subjects[section].indexOf(button.subjectID);
        if (index > -1)
            this.subjects[section].splice(index, 1);

        // Remove tooltip for subject
        if (Ext4.isDefined(this.tooltips[button.subjectID]))
            delete this.tooltips[button.subjectID];

        button.destroy();

        var totalPanel = Ext4.getCmp(this.totalPanelPrefix + section);

        if (this.subjects[section].length > 0) {
            var total = this.subjects[section].length;
            var shown = panel.items.getCount();

            if (shown < total) {
                var btnPanel = Ext4.getCmp(this.btnPanelPrefix + section);
                btnPanel.add(this.getButton(this.subjects[section][shown++], section));
            }

            var shownMsg = '';
            if (shown < total)
                shownMsg = ' (showing ' + shown + ')';

            totalPanel.update('<div class="labkey-button">' + this.totalMessages[section] + ': ' + total + shownMsg + '</div>');
        }
        else {
            // If no more id's in that section, redisplay to remove section
            this.setSubjGrid(false);
        }
    },


    getButton: function (subject, name) {

        return {
            xtype: 'button',
            border: true,
            minWidth: 80,
            height: this.rowHeight,
            text: subject,
            id: this.btnPrefix + subject,
            subjectID: subject,
            tooltipType: 'qtip',
            style: 'margin: 2px;',
            section: name,
            handler: this.buttonHandler,
            listeners: {
                scope: this,
                afterRender: function (btn) {
                    Ext4.create('Ext.tip.ToolTip', {
                        target: btn.getEl(),
                        anchorToTarget: true,
                        html: '<div class="ldk-tooltip">' + this.tooltips[btn.subjectID] +
                        (Ext4.isEmpty(this.tooltips[btn.subjectID]) ? '' : '<br>') + '</div><div class="ldk-tooltip-light">Click to remove.</div>'
                    });
                }
            },
            scope: this
        }
    },

    generateButtons: function (subjects, max, name) {
        var subjButtons = [];

        for (var i = 0; i < subjects.length; i++) {
            subjButtons.push(this.getButton(subjects[i], name));
        }

        return {
            xtype: 'panel',
            id: this.btnPanelPrefix + name,
            border: false,
            items: subjButtons,
            overflowY: 'auto',
            maxHeight: max * (this.rowHeight + 4) + 1,
            layout: {
                type: 'table',
                columns: this.subjectColumns
            }
        };
    },

    removeFromSubjects: function (id, newSubjects) {
        var subjIndex = newSubjects.indexOf(id);

        // Remove from new subjects being added
        if (subjIndex != -1)
            newSubjects.splice(subjIndex, 1);

        // Remove from existing subjects
        for (var i = 0; i < this.subjects[this.btnTypes.subjects].length; i++) {
            if (this.subjects[this.btnTypes.subjects][i] == id) {
                this.subjects[this.btnTypes.subjects].splice(i, 1)
            }
        }

        return newSubjects;
    },

    adjustSpace: function (totalSections) {
        var rowsPerSection = 0, extraRows = 0;

        // This is in order of priority who gets spare rows
        var sections = [{
            name: this.btnTypes.subjects,
            rows: Math.ceil(this.subjects[this.btnTypes.subjects].length / this.subjectColumns),
            rowsDonated: 0
        },
            {
                name: this.btnTypes.aliases,
                rows: Math.ceil(this.subjects[this.btnTypes.aliases].length / this.subjectColumns),
                rowsDonated: 0
            },
            {
                name: this.btnTypes.conflicted,
                rows: Math.ceil(this.subjects[this.btnTypes.conflicted].length / this.subjectColumns),
                rowsDonated: 0
            },
            {
                name: this.btnTypes.notfound,
                rows: Math.ceil(this.subjects[this.btnTypes.notfound].length / this.subjectColumns),
                rowsDonated: 0
            }];

        // Make room for headers
        var maxSubjects = this.maxSubjectsToShow - ((totalSections) * this.subjectColumns);

        // Calculate even space for each section
        if (totalSections != 0)
            rowsPerSection = Math.floor((maxSubjects / this.subjectColumns) / totalSections);

        // Since taking the floor of rowsPerSection there will be remainder rows available
        extraRows = this.subjectMaxRows - totalSections - (rowsPerSection * totalSections);

        Ext4.each(sections, function (section) {
            var totalRows = section.rows;
            if ((section.rows - rowsPerSection) > 0) {
                section.rows = rowsPerSection;

                // Get empty rows from other sections
                Ext4.each(sections, function (check) {
                    if (check.rows == 0 || check.name == section.name)
                        return;

                    var available = rowsPerSection - check.rows - check.rowsDonated;
                    var needed = totalRows - rowsPerSection + section.rowsDonated;
                    if (needed < 1)
                        return;

                    if (available > 0) {
                        var diff = needed - available;
                        if (diff > -1) {
                            section.rows += available;
                            section.rowsDonated -= available;
                            check.rowsDonated += available;
                        }
                        else {
                            section.rows += needed;
                            section.rowsDonated -= needed;
                            check.rowsDonated += needed;
                        }
                    }
                }, this);

                // Remainder rows
                if (totalRows > section.rows && extraRows > 0) {
                    var stillNeeded = totalRows - section.rows;
                    if (stillNeeded >= extraRows) {
                        section.rows += extraRows;
                        extraRows = 0;
                    }
                    else {
                        section.rows += stillNeeded;
                        extraRows -= stillNeeded;
                    }
                }
            }
        }, this);


        var rowCounts = {};
        Ext4.each(sections, function (section) {
            rowCounts[section.name] = section.rows;
        }, this);

        return rowCounts;
    },

    getHeader: function (name, msg, total, shown, first) {
        return {
            xtype: 'panel',
            id: this.totalPanelPrefix + name,
            border: false,
            margins: first ? '0' : '10px 0 0 0',
            html: '<div class="labkey-button">' + msg + ': ' + total + '</div>'
        }
    },

    generateSection: function (subjects, name, rowCounts, msg, first, page) {
        var items = [];
        var shown = subjects.length;

        if (subjects.length > (rowCounts[name] * this.subjectColumns)) {
            shown = rowCounts[name] * this.subjectColumns;
        }

        items.push(this.getHeader(name, msg, subjects.length, shown, first));
        items = items.concat(this.generateButtons(subjects, rowCounts[name], name, page));

        return items;
    },

    sortSubjects: function (subjects, aliases, notfound) {
        if (this.subjects[this.btnTypes.subjects].length > 0) {
            this.subjects[this.btnTypes.subjects] = Ext4.unique(this.subjects[this.btnTypes.subjects]);
            this.subjects[this.btnTypes.subjects].sort();
        }

        if (this.subjects[this.btnTypes.aliases].length > 0) {
            this.subjects[this.btnTypes.aliases] = Ext4.unique(this.subjects[this.btnTypes.aliases]);
            this.subjects[this.btnTypes.aliases].sort();
        }

        if (this.subjects[this.btnTypes.conflicted].length > 0) {
            this.subjects[this.btnTypes.conflicted] = Ext4.unique(this.subjects[this.btnTypes.conflicted]);
            this.subjects[this.btnTypes.conflicted].sort();
        }

        if (this.subjects[this.btnTypes.notfound].length > 0) {
            this.subjects[this.btnTypes.notfound] = Ext4.unique(this.subjects[this.btnTypes.notfound]);
            this.subjects[this.btnTypes.notfound].sort();
        }
    },

    setSubjGrid: function (clear, subjects, aliases, notfound, page) {
        var target = this.down('#idPanel');
        target.removeAll();

        var items = [], aliasId;
        var sections = 0, rowCounts;

        if (clear) {
            this.subjects[this.btnTypes.subjects] = [];
            this.subjects[this.btnTypes.aliases] = [];
            this.subjects[this.btnTypes.conflicted] = [];
            this.subjects[this.btnTypes.notfound] = [];
            this.tooltips = {};
        }

        // All not found subjects
        if (notfound && notfound.length > 0) {
            Ext4.each(notfound, function (subj) {
                this.subjects[this.btnTypes.notfound].push(subj);
                this.tooltips[subj] = 'ID not found.';
            }, this);
        }

        if (aliases) {
            for (var alias in aliases) {
                if (aliases.hasOwnProperty(alias)) {

                    // multiple ID's for one alias
                    if (aliases[alias].length > 1) {
                        Ext4.each(aliases[alias], function (id) {
                            this.subjects[this.btnTypes.conflicted].push(id);
                            this.tooltips[id] = 'Alias: ' + alias;
                            subjects = this.removeFromSubjects(id, subjects);
                        }, this);
                    }
                    else {
                        aliasId = aliases[alias][0];
                        this.subjects[this.btnTypes.aliases].push(aliasId);
                        this.tooltips[aliasId] = 'Alias: ' + alias;
                        subjects = this.removeFromSubjects(aliasId, subjects);
                    }
                }
            }
        }

        // Process new non-alias subjects
        if (subjects && subjects.length > 0) {
            Ext4.each(subjects, function (subj) {
                this.subjects[this.btnTypes.subjects].push(subj);
                this.tooltips[subj] = '';
            }, this);
        }

        this.sortSubjects();

        // Count sections
        if (this.subjects[this.btnTypes.subjects].length > 0)
            sections++;
        if (this.subjects[this.btnTypes.aliases].length > 0)
            sections++;
        if (this.subjects[this.btnTypes.conflicted].length > 0)
            sections++;
        if (this.subjects[this.btnTypes.notfound].length > 0)
            sections++;

        // Adjust rows per section
        rowCounts = this.adjustSpace(sections);

        var first = true;

        // Buttons for non-alias subjects
        if (this.subjects[this.btnTypes.subjects].length > 0) {
            items = items.concat(this.generateSection(this.subjects[this.btnTypes.subjects], this.btnTypes.subjects, rowCounts, this.totalMessages[this.btnTypes.subjects], first, page));
            first = false;
        }

        // Buttons for alias subjects
        if (this.subjects[this.btnTypes.aliases].length > 0) {
            items = items.concat(this.generateSection(this.subjects[this.btnTypes.aliases], this.btnTypes.aliases, rowCounts, this.totalMessages[this.btnTypes.aliases], first, page));
            first = false;
        }

        // Buttons for conflicted aliases
        if (this.subjects[this.btnTypes.conflicted].length > 0) {
            items = items.concat(this.generateSection(this.subjects[this.btnTypes.conflicted], this.btnTypes.conflicted, rowCounts, this.totalMessages[this.btnTypes.conflicted], first, page));
            first = false;
        }

        // Buttons for ID's not found
        if (this.subjects[this.btnTypes.notfound].length > 0) {
            items = items.concat(this.generateSection(this.subjects[this.btnTypes.notfound], this.btnTypes.notfound, rowCounts, this.totalMessages[this.btnTypes.notfound], first, page));
        }

        target.add({
            xtype: 'panel',
            border: false,
            layout: {
                type: 'vbox'
            },
            items: items
        });
    },

    getSubjects: function () {
        var subjects = [];

        for (var section in this.subjects) {
            if (this.subjects.hasOwnProperty(section) && section != this.btnTypes.notfound) {
                Ext4.each(this.subjects[section], function (subject) {
                    subjects.push(subject);
                }, this);
            }
        }
        
        return subjects;
    },

    checkValid: function(){
        if (this.activeFilterType)
            return this.activeFilterType.checkValid();

        return true;
    },

    onSubmit: function(btn){
        if (!this.checkValid())
            return;

        if (btn)
            this.forceRefresh = true;

        this.activeReport = null;
        var tabPanel = this.down('#tabPanel');
        var categoryTab = tabPanel.getActiveTab();
        if (categoryTab){
            var subTab = categoryTab.getActiveTab();
            if (subTab){
                this.activeReport = subTab;
            }
            else {
                if (this.defaultReport){
                    var report = this.findReport(this.defaultReport);
                    if (report){
                        var owner = report.up('tabpanel');
                        if (owner == categoryTab){
                            this.activeReport = report;
                        }
                    }
                }

                //if a top-level tab is active, but no 2nd tier tab selected, use the left-most tab
                if (!this.activeReport && categoryTab){
                    this.activeReport = categoryTab.items.get(0);
                }
            }
        }

        if (this.activeReport){
            var parentTab = this.activeReport.up('tabpanel');
            tabPanel.setActiveTab(parentTab);
            parentTab.setActiveTab(this.activeReport);

            this.loadTab(this.activeReport);
        }
        else {
            Ext4.Msg.alert('Error', 'You must select a report to display by clicking the one of the 2nd tier tabs below.')
        }
    },

    findReport: function(name){
        var tabPanel = this.down('#tabPanel');
        var panel;
        tabPanel.items.each(function(tab){
            panel = tab.down('panel[itemId="' + name + '"]');
            if (panel)
                return false;
        }, this);

        return panel;
    },

    displayReport: function(tab){
        switch (tab.report.reportType){
            case 'query':
                this.loadQuery(tab);
                break;
            case 'details':
                this.loadDetails(tab);
                break;
            case 'report':
                this.loadReport(tab);
                break;
            case 'js':
                this.loadJS(tab);
                break;
            default:
                LDK.Utils.getErrorCallback()({
                    message: 'Improper Report Type'
                });
        }
    },

    getFilterArray: function(tab){
        var report = tab.report;
        var filterArray = this.activeFilterType.getFilterArray(tab);

        return filterArray;
    },

    getCombinedFilterArray: function(tab){
        var fa = this.getFilterArray(tab);
        var ret = [];
        if (fa && fa.removable){
            ret = ret.concat(fa.removable);
        }

        if (fa && fa.nonRemovable){
            ret = ret.concat(fa.nonRemovable);
        }
        
        return ret;
    },


    getTitleSuffix: function(tab){
        var title = this.activeFilterType.getTitle(tab);
        return title ? ' - ' + title : '';
    },

    loadQuery: function(tab){
        var filterArray = this.getFilterArray(tab);

        if (!this.validateReportForFilterType(tab, filterArray))
            return;

        var title = this.getTitleSuffix(tab);

        var queryConfig = {
            title: tab.report.label + title,
            schemaName: tab.report.schemaName,
            queryName: tab.report.queryName,
            suppressRenderErrors: true,
            allowChooseQuery: false,
            allowChooseView: true,
            showInsertNewButton: !!this.allowEditing,
            showDeleteButton: !!this.allowEditing,
            showDetailsColumn: true,
            showUpdateColumn: !!this.allowEditing,
            showRecordSelectors: true,
            showReports: false,
            allowHeaderLock: false, //added b/c locking does not work well inside Ext4 panels
            tab: tab,
            frame: 'portal',
            buttonBarPosition: 'top',
            timeout: 0,
            filters: filterArray.nonRemovable,
            removeableFilters: filterArray.removable,
            linkTarget: '_blank',
            success: this.onDataRegionLoad,
            failure: LDK.Utils.getErrorCallback(),
            scope: this
        };

        //special case these two properties because they are common
        if (tab.report.viewName){
            queryConfig.viewName = tab.report.viewName;
        }

        if (tab.report.containerPath){
            queryConfig.containerPath = tab.report.containerPath;
        }

        //allow any other supported properties to be applied through here
        if (tab.report.queryConfig){
            Ext4.apply(queryConfig, tab.report.queryConfig);
        }

        tab.add({
            xtype: 'ldk-querypanel',
            itemId: 'queryPanel',
            queryConfig: queryConfig
        });
    },

    onDataRegionLoad: function(dr){
        var itemWidth = Ext4.get(dr.domId).getSize().width + 150;
        this.doResize(itemWidth);
        LABKEY.Utils.signalWebDriverTest("LDK_reportTabLoaded");
    },

    onTabChange: function(tab){
        if (tab.items.getCount()){
            var item = tab.items.get(0);
            if (item.onContentSizeChange){
                item.onContentSizeChange();
            }

            var width = item.getWidth();
            this.doResize(width);
        }
        LABKEY.Utils.signalWebDriverTest("LDK_reportTabLoaded");
    },

    doResize: function(itemWidth){
        var width2 = this.getWidth();
        if (itemWidth > width2){
            this.setWidth(itemWidth);
            this.doLayout();
        }
        else if (itemWidth < width2) {
            if (this.originalWidth && width2 != this.originalWidth){
                this.setWidth(Math.max(this.originalWidth, itemWidth));
                this.doLayout();
            }
        }
    },

    getQWPConfig: function(config){
        var ret = {
            allowChooseQuery: false,
            allowChooseView: true,
            showRecordSelectors: true,
            suppressRenderErrors: true,
            allowHeaderLock: false, //added b/c locking does not work well inside Ext4 panels
            showReports: false,
            frame: 'portal',
            linkTarget: '_blank',
            buttonBarPosition: 'top',
            timeout: 0,
            success: this.onDataRegionLoad,
            failure: LDK.Utils.getErrorCallback(),
            scope: this,
            showInsertNewButton: false,
            showDeleteButton: false,
            showDetailsColumn: true,
            showUpdateColumn: false
        };

        if (this.allowEditing){
            Ext4.apply(ret, {
                showInsertNewButton: true,
                showDeleteButton: true,
                showUpdateColumn: true
            });
        }

        Ext4.apply(ret, config);

        return ret;
    },

    loadReport: function(tab){
        var filterArray = this.getFilterArray(tab);

        if (!this.validateReportForFilterType(tab, filterArray))
            return;

        filterArray = filterArray.nonRemovable.concat(filterArray.removable);
        var target = tab.add({
            xtype: 'ldk-contentresizingpanel',
            minHeight: 50
        });

        target.mask('Loading...');

        var queryConfig = {
            partName: 'Report',
            renderTo: target.renderTarget,
            suppressRenderErrors: true,
            partConfig: {
                title: tab.report.label + this.getTitleSuffix(),
                schemaName: tab.report.schemaName,
                reportId : tab.report.reportId,
                'query.queryName': tab.report.queryName
            },
            filters: filterArray,
            success: function(result){
                target.unmask();
                Ext4.defer(target.createListeners, 200, target);
                LABKEY.Utils.signalWebDriverTest("LDK_reportTabLoaded");
            },
            failure: LDK.Utils.getErrorCallback(),
            scope: this
        };

        if (filterArray.length){
            Ext4.each(filterArray, function(filter){
                queryConfig.partConfig[filter.getURLParameterName('query')] = filter.getURLParameterValue();
            }, this);
        }

        if (tab.report.containerPath){
            queryConfig.containerPath = tab.report.containerPath;
        }

        if (tab.report.viewName){
            queryConfig.partConfig.showSection = tab.report.viewName;
        }

        new LABKEY.WebPart(queryConfig).render();
    },

    loadJS: function(tab){
        var jsFunction;
        if (Ext4.isFunction(tab.report.jsHandler)){
            jsFunction = tab.report.jsHandler;
        }
        else {
            //NOTE: namespace is only retained for legacy support.  It should be eliminated.
            var ns = this.reportNamespace;
            jsFunction = ns ? ns[tab.report.jsHandler] : this[tab.report.jsHandler];
        }

        if (!jsFunction)
        {
            var message = "Could not find JavaScript function '" + tab.report.jsHandler + "' to load tab in Animal History. The report is misconfigured.";
            LDK.Utils.logError(message);
            alert(message);
        }
        else
        {
            jsFunction(this, tab);
        }
        LABKEY.Utils.signalWebDriverTest("LDK_reportTabLoaded");
    },

    loadDetails: function(tab, target){
        var filterArray = this.getFilterArray(tab);

        if (~this.validateReportForFilterType(tab, filterArray)){
            return;
        }

        filterArray = filterArray.nonRemovable.concat(filterArray.removable);

        target = tab.add({tag: 'span', html: 'Loading', cls: 'loading-indicator'});

        var config = {
            schemaName: tab.report.schemaName,
            queryName: tab.report.queryName,
            title: tab.report.label + this.getTitleSuffix(),
            titleField: 'Id',
            renderTo: target.id,
            success: function(){
                LABKEY.Utils.signalWebDriverTest("LDK_reportTabLoaded");
            },
            filterArray: filterArray,
            multiToGrid: this.multiToGrid
        };

        if (tab.report.viewName){
            config.viewName = tab.report.viewName;
        }

        Ext4.create('LDK.ext.MultiDetailsPanel', config);
    },

    validateReportForFilterType: function(tab, filterArray){
        var message = this.activeFilterType.validateReport(tab.report);
        if (!message) {
            return true;
        }
        else {
            tab.removeAll();
            tab.add({
                html: message,
                border: false
            });

            return false;
        }
    },

    getFilterOptionsItems: function(){
        var cfg = [{
            width: 200,
            html: '<p>Type of Search:</p>'
        },{
            xtype: 'radiogroup',
            itemId: 'inputType',
            labelWidth: 200,
            defaults: {
                width: 200
            },
            columns: 1,
            listeners: {
                scope: this,
                change: function(field, val){
                    val = val.selector;
                    this.changeFilterType(val);
                }
            },
            items: []
        }];

        Ext4.each(this.filterTypes, function(t, idx){
            cfg[1].items.push({
                xtype: 'radio',
                name: 'selector',
                inputValue: t.inputValue,
                checked: idx == 0,
                boxLabel: t.label,
                hidden: t.hidden,
                value: t.initialValue
            });
        }, this);

        return cfg;
    },

    changeFilterType: function(inputValue){
        var target = this.down('#filterPanel');

        var filterType = this.getFilterType(inputValue);
        if (filterType){
            var cfg = Ext4.apply({}, filterType);
            cfg.tabbedReportPanel = this;
            cfg.filterContext = this.getFilterContext();

            if (this.activeFilterType){
                this.activeFilterType.prepareRemove();
            }
            target.removeAll();

            this.activeFilterType = target.add(cfg);
        }

        if (this.loadOnRender || this.autoLoadDefaultTab){
            this.onSubmit();
            this.loadOnRender = null;
            this.autoLoadDefaultTab = null;
        }
    },

    getFilterContext: function(){
        var ctx;
        if (this.activeFilterType){
            ctx = this.activeFilterType.getFilterArray(this.activeReport || this.findReport(this.defaultReport));
        }

        ctx = ctx || {};

        if (this.initialContext){
            Ext4.applyIf(ctx, this.initialContext);
            this.initialContext = null;
        }

        return ctx;
    },

    getFilterType: function(inputValue){
        var filter;
        Ext4.each(this.filterTypes, function(f){
            if (f.inputValue == inputValue){
                filter = f;
                return false;
            }
        }, this);

        return filter ? Ext4.apply({}, filter) : null;
    },

    getFiltersFromUrl: function(){
        var context = {};
        if (document.location.hash){
            var token = document.location.hash.split('#');
            token = token[1].split('&');

            for (var i=0;i<token.length;i++){
                var t = token[i].split(':');
                switch(t[0]){
                    case 'inputType':
                        context.inputType = t[1];
                        break;
                    case 'showReport':
                        this.loadOnRender = (t[1] == 1);
                        break;
                    case 'activeReport':
                        this.report = t[1];
                        var tab = this.reportMap[t[1]];
                        if (tab){
                            this.activeReport = tab;
                            this.silentlySetActiveTab(this.activeReport);
                        }
                        else {
                            console.error('unable to find tab: ' + t[1])
                        }
                        break;
                    default:
                        context[t[0]] = t[1];
                }
            }
        }

        return context;
    },

    createTabPanel: function(){
        var tabPanel = this.down('#tabPanel');

        if (!this.reports || !this.reports.length){
            tabPanel.add({
                html: 'There are no reports enabled in this folder.  Please contact your administrator if you believe this is an error.',
                style: 'padding: 10px;',
                border: false
            });
            return;
        }

        //if there is only one category, simplify the output
        if (this.getDistinctCategories().length == 1){
            if (tabPanel.rendered){
                tabPanel.down('tabbar').setVisible(false);
            }
            else {
                tabPanel.tabBar = tabPanel.tabBar || {};
                tabPanel.tabBar.hidden = true;
            }
        }
        
        Ext4.each(this.reports, function(report){
            if (!report || !report.category)
                return;

            if (report.visible === false)
                return;

            var category = report.category;

            //create top-level tab
            if (!tabPanel.down('panel[itemId="' + category + '"]')){
                tabPanel.add({
                    xtype: 'tabpanel',
                    style: 'margin-bottom: 10px;',
                    itemId: category,
                    title: category,
                    enableTabScroll: true,
                    listeners: {
                        scope: this,
                        tabchange: function(panel, tab, oldTab){
                            this.activeReport = tab;
                            this.silentlySetActiveTab(this.activeReport);
                            //jQuery(".report-tab-bar").removeClass("report-tab-bar");
                            //panel.getTabBar().addCls("report-tab-bar");
                            this.onSubmit();
                        },
                        added: function(panel) {
                            panel.getTabBar().addCls("report-tab-bar");
                        }
                    }
                });
            }

            var subTab = tabPanel.down('panel[itemId="' + category + '"]');
            var reportId = report.id || report.name;

            //create 2nd tier tab
            if (!subTab.down('panel[itemId="' + reportId + '"]')){
                var theTab = subTab.add({
                    xtype: 'panel',
                    style: 'margin-bottom: 10px;',
                    title: report.label,
                    itemId: reportId,
                    report: report,
                    bodyStyle:'padding:5px',
                    border: false,
                    subjectArray: [],
                    filterArray: {},
                    tbar: {
                        style: 'padding-left:10px'
                    }
                });

                if (this.report == reportId){
                    this.activeReport = theTab;
                }

                this.reportMap[reportId] = theTab;
            }
        }, this);

        if (this.activeReport){
            this.silentlySetActiveTab(this.activeReport);
        }
        else if (this.defaultReport){
            var report = this.findReport(this.defaultReport);
            if (report)
                this.silentlySetActiveTab(report);
        }
        else if (this.defaultTab) {
            var tab = tabPanel.down('#' + this.defaultTab);
            tabPanel.suspendEvents();
            tab.suspendEvents();
            tabPanel.setActiveTab(tab);
            tab.resumeEvents();
            tabPanel.resumeEvents();
        }

        //populate initial fields
        var shouldChange = true;
        this.initialContext = this.getFiltersFromUrl();
        var filterType = this.initialContext.inputType;
        if (filterType){
            var radio = this.down('#inputType');
            var val = radio.getValue().selector;

            radio.setValue({
                selector: filterType
            });

            if (filterType != val)
                shouldChange = false;
        }

        if (shouldChange) {
            this.changeFilterType(this.down('#inputType').getValue().selector);
        }

        var submitBtn = this.down('#submitBtn');
        if (submitBtn){
            this.tabsReady = true;
            submitBtn.setDisabled(false);
            LABKEY.Utils.signalWebDriverTest("LDK_reportPanelLoaded");
        }
    },

    createKeyListener: function(el){
        Ext4.create('Ext.util.KeyNav', el, {
            scope: this,
            enter: this.onSubmit
        });
    },

    silentlySetActiveTab: function(tab){
        var tabPanel = this.down('#tabPanel');

        tabPanel.suspendEvents();
        tab.suspendEvents();
        tab.ownerCt.suspendEvents();

        tab.ownerCt.setActiveTab(tab);
        tabPanel.setActiveTab(tab.ownerCt);

        tab.resumeEvents();
        tab.ownerCt.resumeEvents();
        tabPanel.resumeEvents();
    },

    updateTab: function (filters) {
        var tab = this.activeReport || this.findReport(this.defaultReport);
        var reload = false;

        filters = this.filterHistory(tab, filters);

        if (tab.filters){
            for (var i in filters){
                if (JSON.stringify(filters[i]) !== JSON.stringify(tab.filters[i])){
                    reload = true;
                    break;
                }
            }
        }
        else {
            reload = true;
        }

        //indicates tab already has up to date content
        if (reload == false && !this.forceRefresh){
            this.onTabChange(tab);
            console.log('no reload needed');
            return;
        }
        this.forceRefresh = null;

        tab.filters = filters;
        tab.removeAll();

        this.activeReport = tab;
        tab.hasLoaded = true;
        this.hasLoaded = true;
        this.displayReport(tab);
    },

    filterHistory: function (tab, filters) {

        Ext4.apply(filters, {
            inputType : this.down('#inputType').getValue().selector,
            showReport: 1,
            activeReport: tab.report.id
        });

        //set history
        var token = [];
        for (var i in filters){
            if (!filters.hasOwnProperty(i)) {
                continue;
            }
            if (filters[i]){
                // NOTE: requests will fail if the URL is too long.  when trying to filter on a long list of discrete IDs, it can be fairly easy to hit this limit
                // this solution isnt perfect, but if we simply omit those IDs from the return URL it should succeed.  this will
                // mean we cannot
                if (filters[i].length > 200){
                    console.log('param is too long for URL: ' + i + '/' + filters[i].length);
                }
                else {
                    token.push(i + ':' + filters[i]);
                }
            }
        }
        Ext4.History.add(token.join('&'));

        return filters;
    },

    loadTab: function (tab) {
        if (this.activeFilterType.loadReport) {
            this.activeFilterType.loadReport(tab, this.updateTab, this);
        }
        else {
            this.updateTab(this.activeFilterType.getFilters() || {});
        }
    }
});