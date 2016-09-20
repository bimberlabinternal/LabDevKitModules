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
    maxSubjectsToShow: 15,
    showDiscvrLink: true,

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
                text: 'Refresh',
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

    setSubjGrid: function(subjects){
        var target = this.down('#idPanel');
        target.removeAll();

        if (subjects && subjects.length){
            target.add({
                itemId: 'totalPanel',
                html: 'Total IDs: ' + subjects.length
            });

            var toAdd = [];
            for (var i = 0; i < Math.min(this.maxSubjectsToShow, subjects.length); i++){
                toAdd.push({
                    xtype: 'button',
                    border: true,
                    minWidth: 80,
                    text: subjects[i]+' (X)',
                    subjectID: subjects[i],
                    style: 'margin: 2px;',
                    handler: function(button){
                        var panel = button.up('#buttonPanel');
                        var index = panel.subjectIDs.indexOf(button.subjectID);
                        if(index > -1) {
                            panel.subjectIDs.splice(index, 1);
                        }

                        button.destroy();

                        var total = panel.items.getCount();
                        var owner = panel.up('panel');
                        var div = owner.down('#totalPanel');
                        div.destroy();
                        owner.insert(0, {
                            itemId: 'totalPanel',
                            html: 'Total IDs: ' + total
                        });
                    },
                    scope: this
                });
            }

            if (toAdd.length){
                target.add({
                    xtype: 'panel',
                    itemId: 'buttonPanel',
                    subjectIDs: subjects,
                    layout: {
                        type: 'table',
                        columns: 5
                    },
                    items: toAdd
                });
            }

            if (subjects.length > this.maxSubjectsToShow) {
                target.add({
                    xtype: 'panel',
                    border: false,
                    html: '<span class="labkey-error">Plus ' + (subjects.length - this.maxSubjectsToShow)  + ' additional IDs</span>'
                });
            }
        }
    },

    getSubjects: function(){
        var panel = this.down('#buttonPanel');
        if (!panel || !panel.items.getCount())
            return null;

        var subjects = [];

        if (panel.subjectIDs)
            subjects = subjects.concat(panel.subjectIDs);
        else
        {
            panel.items.each(function (btn) {
                if (btn.subjectID)
                    subjects.push(btn.subjectID);
            }, this);
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
            ctx = this.activeFilterType.getFilters();
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

    loadTab: function(tab){
        var filters = this.getFilters(tab);

        var reload = false;
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

    getFilters: function(tab){
        var filters = this.activeFilterType.getFilters() || {};
        Ext4.apply(filters, {
            inputType : this.down('#inputType').getValue().selector,
            showReport: 1,
            activeReport: tab.report.id
        });

        //set history
        var token = [];
        for (var i in filters){
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
    }
});