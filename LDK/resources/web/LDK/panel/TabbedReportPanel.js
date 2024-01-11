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
    extend: 'Ext.container.Container',
    alias: 'widget.ldk-tabbedreportpanel',
    cls: 'ldk-tabbed-report-panel',
    allowEditing: true,
    showDiscvrLink: true,
    subjectColumns: 8,
    subjectMaxRows: 9,
    rowHeight: 26,
    widthPadding: 35,
    initializing: true,
    isReportTabSelected: false,
    autoLoadDefaultTab: false,
    clearBetweenClicks: true,

    // Passed to filters implementing caseInsensitive id search
    caseInsensitiveSubjects: false,

    // Passed to filters implementing reportQCStates filter
    reportQCStates: [],

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
    showFilterOptionsTitle: false,
    showReportsOption: false,

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
                bodyStyle: 'background-color : transparent;margin-bottom: 10px;',
                items: [{
                    xtype: 'panel',
                    defaults: {
                        border: false
                    },
                    cls: 'ldk-report-filter-options',
                    title: this.showFilterOptionsTitle ? 'Filter Options' : undefined,
                    titleCollapse: this.showFilterOptionsTitle,
                    bodyStyle: this.showFilterOptionsTitle ? 'padding: 5px;' : undefined,
                    collapsible: this.showFilterOptionsTitle,
                    minWidth: this.showFilterOptionsTitle ? 600 : undefined,
                    border: this.showFilterOptionsTitle,
                    margin: this.showFilterOptionsTitle ? '0 20px 0 0' : undefined,
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
                    },{
                        xtype: 'button',
                        border: true,
                        text: 'Update Report',
                        handler: this.userInitiatedOnSubmit,
                        itemId: 'submitBtn',
                        disabled: true,
                        scope: this,
                        style:'margin-left:200px;margin-top: 10px;'
                    }]
                },{
                    itemId: 'idPanel',
                    cls: 'ldk-report-filter-id-panel',
                    border: false,
                    defaults: {
                        border: false
                    }
                }]
            }]
        });

        this.addEvents('tabchange');
        this.on('tabchange', this.afterTabChange, this, {buffer: 50});

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

        Ext4.QuickTips.init();

        this.callParent(arguments);

        this.on('afterrender', this.onAfterRender);
    },

    doResizeCmp: function(cmp, width, height, oldWidth, oldHeight) {
        this.doLayout();

        var max = window.innerWidth - this.widthPadding;

        // Prevent infinite loop of resizing
        if (width !== (oldWidth || 0) + this.widthPadding) {

            // if old width is zero then a new webpart is being added so start over at normal window width
            if (oldWidth) {
                // Look at webpart headers to determine if any are wider than window
                var hdrs = Ext4.select('.panel-heading');
                var hdrWidth = 0;
                hdrs.elements.forEach(function (hdr) {
                    hdrWidth = hdr.offsetWidth + this.widthPadding;
                    if (hdrWidth > max)
                        max = hdrWidth;
                }, this);
            }

            this.resizeWidth(max);
        }
    },

    resizeWidth: function(newWidth, oldWidth){
        var ul = Ext4.select('div#bs-category-tabs-list .nav-tabs');
        var bsWidth = 0;
        if (ul && ul.elements && ul.elements.length > 0) {
            bsWidth = ul.elements[0].offsetWidth;
        }

        // If window narrower than top tabs
        if (!newWidth || newWidth < bsWidth)
            newWidth = bsWidth;

        if (this.getWidth() !== newWidth) {
            this.setWidth(newWidth);
        }
    },

    onAfterRender: function(panel){
        this.originalWidth = this.getWidth();
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

            totalPanel.update('<div class="ldk-total-message-header">' + this.totalMessages[section] + ': ' + total + shownMsg + '</div>');
        }
        else {
            // If no more id's in that section, redisplay to remove section
            this.setSubjGrid(false, Ext4.isDefined(this.activeFilterType.aliasTable));
        }
    },


    getButton: function (subject, name) {

        return {
            xtype: 'button',
            border: true,
            minWidth: 80,
            height: this.rowHeight,
            text: subject,
            icon: LABKEY.ActionURL.getContextPath() + '/ext-4.2.1/resources/ext-theme-classic-sandbox/images/tools/tool-sprites.gif',
            iconCls: 'closeicon',
            iconAlign : 'right',
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
            autoScroll: true,
            maxHeight: max * (this.rowHeight + 4) + 1,
            maxWidth: this.getWidth() - 500,
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
            html: '<div class="ldk-total-message-header">' + msg + ': ' + total + '</div>'
        }
    },

    generateSection: function (subjects, name, rowCounts, msg, first) {
        var items = [];
        var shown = subjects.length;

        if (subjects.length > (rowCounts[name] * this.subjectColumns)) {
            shown = rowCounts[name] * this.subjectColumns;
        }

        items.push(this.getHeader(name, msg, subjects.length, shown, first));
        items = items.concat(this.generateButtons(subjects, rowCounts[name], name));

        return items;
    },

    sortSubjects: function () {
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

    setSubjGrid: function (clear, aliasCheck, subjects, aliases, notfound) {
        var target = this.down('#idPanel');
        target.removeAll();
        if(Ext4.isDefined(this.activeFilterType) && Ext4.isFunction(this.activeFilterType.clearSubjectArea))
            this.activeFilterType.clearSubjectArea();

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
            var title = aliasCheck?this.totalMessages[this.btnTypes.subjects]:"Total IDs";
            items = items.concat(this.generateSection(this.subjects[this.btnTypes.subjects], this.btnTypes.subjects, rowCounts, title, first));
            first = false;
        }

        // Buttons for alias subjects
        if (this.subjects[this.btnTypes.aliases].length > 0) {
            items = items.concat(this.generateSection(this.subjects[this.btnTypes.aliases], this.btnTypes.aliases, rowCounts, this.totalMessages[this.btnTypes.aliases], first));
            first = false;
        }

        // Buttons for conflicted aliases
        if (this.subjects[this.btnTypes.conflicted].length > 0) {
            items = items.concat(this.generateSection(this.subjects[this.btnTypes.conflicted], this.btnTypes.conflicted, rowCounts, this.totalMessages[this.btnTypes.conflicted], first));
            first = false;
        }

        // Buttons for ID's not found
        if (this.subjects[this.btnTypes.notfound].length > 0) {
            items = items.concat(this.generateSection(this.subjects[this.btnTypes.notfound], this.btnTypes.notfound, rowCounts, this.totalMessages[this.btnTypes.notfound], first));
        }

        target.add({
            xtype: 'panel',
            id: 'subjectButtonPanel',
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
            if (this.subjects.hasOwnProperty(section) && section !== this.btnTypes.notfound) {
                Ext4.each(this.subjects[section], function (subject) {
                    subjects.push(subject);
                }, this);
            }
        }

        return subjects;
    },

    checkFiltersValid: function(){
        if (this.activeFilterType)
            return this.activeFilterType.isValid();

        return true;
    },

    determineActiveReport: function(){
        var activeReport = null;
        var tabPanel = this.getTabPanel();
        var categoryTab = tabPanel.findItemForActiveTabId();
        if (categoryTab) {
            var subTab = categoryTab.items[0].findItemForActiveTabId();
            if (subTab) {
                activeReport = subTab;
            }
            else {
                if (this.defaultReport) {
                    var report = this.findReport(this.defaultReport);
                    if (report) {
                        var owner = this.getCategoryTab(report.id);
                        if (owner === categoryTab) {
                            activeReport = report;
                        }
                    }
                }

                //if a top-level tab is active, but no 2nd tier tab selected, use the left-most tab
                if (!activeReport && categoryTab) {
                    activeReport = categoryTab.items[0].items[0];
                }
            }
        }

        return activeReport;
    },

    possiblySetActiveTabAndLoad: function(forceRefresh){
        this.activeReport = this.determineActiveReport();
        if (!this.activeReport) {
            Ext4.Msg.alert('Error', 'You must select a report to display by clicking the one of the 2nd tier tabs below.');
            return;
        }

        //assuming the user has not yet intiated any report loading, dont try to load, which might cause
        //alters if they havent filled out fields.  however, if the filters are valid, go ahead and load.
        var filtersValid = this.checkFiltersValid();
        if (!filtersValid && !this.isReportTabSelected) {
            return;
        }

        if (!filtersValid) {
            Ext4.Msg.alert('Error', this.activeFilterType.getFilterInvalidMessage());
            return;
        }

        if (!this.activeFilterType) {
            return;
        }

        this.generateFiltersAndLoadTab(forceRefresh);
    },

    generateFiltersAndLoadTab: function (forceRefresh) {
        var tab = this.activeReport;
        if (!tab){
            return;
        }

        forceRefresh = !!forceRefresh;

        if (this.activeFilterType.loadReport) {
            this.activeFilterType.loadReport(tab, this.possiblyUpdateActiveReport, this, forceRefresh);
        }
        else {
            this.possiblyUpdateActiveReport(this.activeFilterType.getFilters() || {}, forceRefresh);
        }
    },

    //updates the provided tab, unless already up-to-date
    possiblyUpdateActiveReport: function (filters, forceRefresh) {
        var tab = this.activeReport;
        var reload = false;

        filters = this.filterHistory(tab, filters);

        var reportTab = tab.items[0];
        if (reportTab.filters){
            for (var i in filters){
                // The intent of this code is to test the filter conditions of this report (i.e. IDs, groups).
                // showReport has nothing to do with this
                if (i === 'showReport') {
                    continue;
                }
                if (JSON.stringify(filters[i]) !== JSON.stringify(reportTab.filters[i])){
                    reload = true;
                    break;
                }
            }
        }
        else {
            reload = true;
        }

        //indicates tab already has up to date content
        if (reload === false && !forceRefresh && !this.clearBetweenClicks){
            //TODO: fix or remove
            if (reportTab.down('ldk-contentresizingcmp')) {
                //reportTab.down('ldk-contentresizingcmp').onContentSizeChange();
                this.doLayout();
            }
            this.signalWebdriverReportTabLoaded();
            return;
        }

        reportTab.filters = filters;
        reportTab.removeAll();

        //note: this will signal to webdriver when content actually loads
        this.displayReport(reportTab);
    },

    findReport: function(name){
        var tabPanel = this.getTabPanel();
        var panel = null, check = null, tab = null;
        for(var i = 0; i < tabPanel.items.length; i++) {
            tab = tabPanel.items[i];
            check = this.getReportPanel(tab, name);
            if (check !== null) {
                panel = check;
                break;
            }
        }

        return panel;
    },

    displayReport: function(tab){
        // If we have a status to show the user to help set expectations, display it at the top
        if (tab.report.reportStatus) {
            var status = Ext4.create('Ext.Component',{
                itemId: 'reportStatus',
                html: '<div class="alert alert-warning" role="alert">Report Status: <strong>'
                + Ext4.util.Format.htmlEncode(tab.report.reportStatus)
                + '</strong></div>'
            });

            tab.add(status);
        }

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

        tab.add({
            html:"<div id='reporttype-" + tab.report.reportType + "' style=\"display: none;\"/>",
            hidden:true
        });
    },

    getFilterArray: function(tab){
        return this.activeFilterType.getFilterArray(tab);
    },

    getTitleSuffix: function(tab){
        var title = this.activeFilterType.getTitle(tab);
        return title ? ' - ' + title : '';
    },

    loadQuery: function(tab){
        if (!this.validateReportForFilterType(tab))
            return;

        var filterArray = this.getFilterArray(tab);
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
            showReports: this.showReportsOption,
            allowHeaderLock: false, //added b/c locking does not work well inside Ext4 panels
            tab: tab,
            frame: 'portal',
            buttonBarPosition: 'top',
            timeout: 0,
            filters: filterArray.nonRemovable,
            removeableFilters: filterArray.removable,
            linkTarget: '_blank',
            success: this.onReportSuccessfulLoad,
            failure: LDK.Utils.getErrorCallback(),
            scope: this
        };

        //
        // Define a list of configuration options.  These can be set on the report tab to override default values.
        // This list can be obtained by going to the URL for the
        // LABKEY.QueryWebPart API, and looking in the config summary table.  If you'd like to get it programatically,   // https://www.labkey.org/download/clientapi_docs/javascript-api/symbols/LABKEY.QueryWebPart.html#constructor
        // run the function commented out to the right of this message (you'll need to load jQuery first).  It will      //
        // output an array of configuration options.                                                                     //getConfigOptions = function() {
        //                                                                                                               //    var summaryTable = jQuery('table.summaryTable')
        var configOptions = [                                                                                            //            // Filter for the table with the config object
            "aggregates",             "allowChooseQuery",     "allowChooseView",        "bodyClass",                     //            .filter(function(index,element){ return jQuery(element).attr("summary").match(/config object/); });
            "buttonBar",              "buttonBarPosition",    "containerFilter",        "containerPath",                 //
            "dataRegionName",         "deleteURL",            "detailsURL",             "errorType",                     //    var options = [];
            "failure",                "filters",              "frame",                  "importURL",                     //
            "insertURL",              "linkTarget",           "maskEl",                 "maxRows",                       //    var configRegex = /^config\./;
            "metadata",               "offset",               "parameters",             "queryName",                     //    summaryTable.find('td.nameDescription a').each(function(index,element){
            "removeableFilters",      "removeableSort",       "renderTo",               "reportId",                      //        var configOption = jQuery(element).text();
            "schemaName",             "scope",                "shadeAlternatingRows",   "showBorders",                   //
            "showDeleteButton",       "showDetailsColumn",    "showExportButtons",      "showInsertNewButton",           //
            "showPagination",         "showRecordSelectors",  "showReports",            "showRows",                      //        if (configOption.match(configRegex)) {
            "showSurroundingBorder",  "showUpdateColumn",     "showViewPanel",          "sort",                          //            options.push(configOption.replace(configRegex, ''));
            "sql",                    "success",              "suppressRenderErrors",   "timeout",                       //        }
            "title",                  "titleHref",            "updateURL",              "viewName"                       //    });
        ];                                                                                                               //

        Ext4.each(configOptions, function(option, index, list) {
            if (option in tab.report) {
                queryConfig[option] = tab.report[option];
            }
        });

        tab.add({
            xtype: 'ldk-querycmp',
            itemId: 'queryPanel',
            queryConfig: queryConfig
        });
    },

    onReportSuccessfulLoad: function(dr){
        this.signalWebdriverReportTabLoaded();
    },

    getQWPConfig: function(config){
        var ret = {
            allowChooseQuery: false,
            allowChooseView: true,
            showRecordSelectors: true,
            suppressRenderErrors: true,
            allowHeaderLock: false, //added b/c locking does not work well inside Ext4 panels
            showReports: this.showReportsOption,
            frame: 'portal',
            linkTarget: '_blank',
            buttonBarPosition: 'top',
            timeout: 0,
            success: this.onReportSuccessfulLoad,
            failure: LDK.Utils.getErrorCallback(),
            scope: this,
            showInsertNewButton: false,
            showImportDataButton: false,
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
        if (!this.validateReportForFilterType(tab))
            return;

        var filterArray = this.getFilterArray(tab);
        filterArray = filterArray.nonRemovable.concat(filterArray.removable);

        var target = Ext4.create('LDK.panel.ContentResizingPanel', {minHeight: 50});
        tab.add(target);

        target.mask('Loading...');

        var queryConfig = {
            partName: 'Report',
            renderTo: target.renderTarget,
            suppressRenderErrors: true,
            partConfig: {
                title: tab.report.label + this.getTitleSuffix(),
                schemaName: tab.report.schemaName,
                reportId : tab.report.reportId,
                'query.queryName': tab.report.queryName,
            },
            filters: filterArray,
            success: function(result){
                target.unmask();
                Ext4.defer(target.createListeners, 200, target);
                this.signalWebdriverReportTabLoaded();
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

        if (!jsFunction) {
            var message = "Could not find JavaScript function '" + tab.report.jsHandler + "' to load tab in TabbedReportPanel. The report is misconfigured.";
            LDK.Utils.logError(message);
            alert(message);
        }
        else {
            jsFunction(this, tab);
        }

        this.signalWebdriverReportTabLoaded();
    },

    signalWebdriverReportTabLoaded: function(){
        Ext4.defer(function(){
            LABKEY.Utils.signalWebDriverTest("LDK_reportTabLoaded");
        }, 200, this);

    },

    loadDetails: function(tab, target){
        if (this.validateReportForFilterType(tab)){
            return;
        }

        var filterArray = this.getFilterArray(tab);
        filterArray = filterArray.nonRemovable.concat(filterArray.removable);

        target = Ext4.create('Ext.Component', {tag: 'span', html: 'Loading', cls: 'loading-indicator'});
        tab.add(target);

        var config = {
            schemaName: tab.report.schemaName,
            queryName: tab.report.queryName,
            title: tab.report.label + this.getTitleSuffix(),
            titleField: 'Id',
            renderTo: target.id,
            scope: this,
            success: this.signalWebdriverReportTabLoaded,
            filterArray: filterArray,
            multiToGrid: this.multiToGrid
        };

        if (tab.report.viewName){
            config.viewName = tab.report.viewName;
        }

        Ext4.create('LDK.ext.MultiDetailsPanel', config);
    },

    validateReportForFilterType: function(tab){
        var message = this.activeFilterType.validateReportForFilterType(tab.report);
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
                inputAttrTpl: 'name = ' + t.inputValue,
                inputValue: t.inputValue,
                checked: idx === 0,
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
            cfg.caseInsensitive = this.caseInsensitiveSubjects;
            cfg.reportQCStates = this.reportQCStates;

            if (this.activeFilterType){
                this.activeFilterType.prepareRemove();
            }
            target.removeAll();

            this.activeFilterType = target.add(cfg);
            LABKEY.Utils.signalWebDriverTest('filterTypeUpdate', this.activeFilterType.inputValue);
        }

        //clear report so it forces content reload
        if (this.activeReport) {
            this.activeReport.filters = null;
        }
    },

    getFilterContext: function(){
        var ctx;
        if (this.activeFilterType){
            var tab = this.activeReport || this.findReport(this.defaultReport);
            if (tab && tab.items && tab.items.length > 0) {
                //if a message is returned, this indicates it is not supported
                ctx = !this.activeFilterType.validateReportForFilterType(this.activeReport) ? this.activeFilterType.getFilterArray(tab.items[0]) : null;
            }
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
            if (f.inputValue === inputValue){
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
                t[0] = decodeURIComponent(t[0]);
                if (t.length > 1) {
                    t[1] = decodeURIComponent(t[1]);
                }
                switch(t[0]){
                    case 'inputType':
                        context.inputType = t[1];
                        break;
                    case 'showReport':
                        this.isReportTabSelected = (t[1] === '1');
                        break;
                    case 'activeReport':
                        var report = t[1];
                        var tab = this.reportMap[report];
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

    getTabItem: function(id, items) {
        var tab = null;

        if (items) {
            items.forEach(function (item) {
                if (item.itemId === id) {
                    tab = item;
                }
            }, this);
        }

        return tab;
    },

    updateTabItem: function(newItem, items) {
        var found = false;

        // Replace if exists
        var newItems = items.map(function (item) {
            if (item.itemId === newItem.itemId) {
                found = true;
                return newItem;
            }

            return item;
        });

        // Add if not exists
        if (!found) {
            newItems.push(newItem);
        }

        return newItems;
    },

    createReportTabPanels: function(items) {
        var me = this;
        return items.map(function(item) {
            if (item.items && item.items.length > 0) {
                item.items = [Ext4.create('LABKEY.ext4.BootstrapTabPanel', {
                    usePills: true,
                    items: item.items,
                    updateHistory: false,
                    listId: 'bs-report-tabs-list',
                    changeHandler: me.onTabChange,
                    resizeHandler: me.doResizeCmp,
                    delayedLayout: false,
                    clearBetweenClicks: this.clearBetweenClicks,
                    scope: me
                })];
            }
            return item;
        })
    },

    getTabPanel: function() {
        var tabPanel = null;
        if (this.items && this.items.items && this.items.items.length > 0) {

            for(var i=0; i<this.items.items.length; i++) {
                if(this.items.items[i].itemId === 'tabPanel') {
                    tabPanel = this.items.items[i];
                    break;
                }
            }
        }

        return tabPanel;
    },

    getReportPanel: function(tab, itemId) {
        var reportPanel = null;
        if (tab && tab.items && tab.items.length > 0 && itemId) {

            var items = tab.items[0].items;

            for (var i=0; i<items.length; i++) {
                if (items[i].itemId.toUpperCase() === itemId.toUpperCase()) {
                    reportPanel = items[i];
                    break;
                }
            }
        }

        return reportPanel;
    },

    getCategoryTab: function(reportTabId) {
        var tabPanel = this.getTabPanel();
        var tab = null;
        var categoryTab = null;

        for (var i=0; i<tabPanel.items.length; i++) {
            tab = tabPanel.items[i];

            if (tab.items && tab.items[0] && tab.items[0].items) {
                for (var j=0; j<tab.items[0].items.length; j++) {
                    if (tab.items[0].items[j].id === reportTabId) {
                        categoryTab = tab;
                        break;
                    }
                }
            }

            if (categoryTab !== null) {
                break;
            }
        }

        return categoryTab;
    },

    createTabPanel: function(){
        if (!this.reports || !this.reports.length){
            this.items.items.push(Ext4.create('Ext.container.Container', {
                html: 'There are no reports enabled in this folder.  Please contact your administrator if you believe this is an error.',
                style: 'padding: 20px;',
                border: false
            }));

            this.doLayout();
            return;
        }

        var items = [];
        Ext4.each(this.reports, function(report){
            if (!report || !report.category)
                return;

            if (report.visible === false)
                return;

            var category = report.category;
            var item = this.getTabItem(category, items);

            if (item === null) {
                item = {
                    title: category,
                    itemId: category,
                    tabType: 'categoryTab',
                    items: []
                };
            }

            var reportId = report.id || report.name;

            //create 2nd tier tab
            var reportItem = {
                title: report.label,
                itemId: reportId,
                tabType: 'reportTab',
                category: report.category,
                scope: this,
                items: [Ext4.create('Ext.container.Container', {
                    report: report,
                    padding: '10px 0',
                    border: false,
                    subjectArray: [],
                    filterArray: {}
                })]
            };

            item.items.push(reportItem);

            this.reportMap[reportId] = reportItem;

            items = this.updateTabItem(item, items);

        }, this);

        var me = this;
        this.items.items.push(Ext4.create('LABKEY.ext4.BootstrapTabPanel', {
            itemId: 'tabPanel',
            usePills: false,
            changeHandler: me.onTabChange,
            updateHistory: false,
            scope: me,
            listId: 'bs-category-tabs-list',
            containerCls: 'bs-tabpanel-content-container',
            delayedLayout: false,
            items: this.createReportTabPanels(items)
        }));

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

            if (filterType !== val)
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
            enter: this.userInitiatedOnSubmit
        });
    },

    silentlySetActiveTab: function(tab){
        this.isReportTabSelected = false;

        var tabPanel = this.getTabPanel();
        var categoryTab = this.getCategoryTab(tab.id);

        tabPanel.suspendEvents();
        if (categoryTab && categoryTab.items) {
            categoryTab.items[0].suspendEvents();
        }

        tabPanel.setActiveTab(categoryTab.id);
        if (categoryTab && categoryTab.items) {
            categoryTab.items[0].setActiveTab(tab.id);
        }

        tabPanel.resumeEvents();
        if (categoryTab && categoryTab.items) {
            categoryTab.items[0].resumeEvents();
        }
    },

    //called when either a top-level of report tab changes:
    onTabChange: function(tab, initializing, evt){
        if (!tab){
            return;
        }

        // If the user actively clicks on a report tab, set to true, forcing report load
        // otherwise the report will only load if filters are valid.  this avoids excess alerts
        this.isReportTabSelected = false;
        if (tab.items[0] && tab.tabType === 'reportTab' && evt) {
            this.isReportTabSelected = true;
        }

        if (!this.findReport || !this.getFilterContext) {
            console.warn("There is a problem in the tab panel change handler. Scope is incorrect.");
            return;
        }

        this.fireEvent('tabchange');
    },

    //find the active report and possibly trigger reload.  this allows events to be buffered
    afterTabChange: function(){
        this.possiblySetActiveTabAndLoad();
    },

    userInitiatedOnSubmit: function(){
        this.isReportTabSelected = true;
        this.possiblySetActiveTabAndLoad(true);
    },

    filterHistory: function (tab, filters) {
        if (tab && tab.items && tab.items.length > 0) {
            Ext4.apply(filters, {
                inputType: this.down('#inputType').getValue().selector,
                showReport: this.isReportTabSelected ? '1' : '0',
                activeReport: tab.items[0].report.id
            });
        }

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
                else if (['removable', 'nonRemovable'].indexOf(i) !== -1){

                }
                else {
                    token.push(i + ':' + filters[i]);
                }
            }
        }

        // Since we're not listening for URL navigation events and updating the selected tabs to match, update
        // the URL with the current set of filters and reports in a way that doesn't add to the browser's history
        location.replace("#" + token.join('&'));

        return filters;
    }
});
