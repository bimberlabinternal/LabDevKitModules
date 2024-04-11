/*
 * Copyright (c) 2011-2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * This class extends Ext.panel.Panel.  The intent is to provide a flexible helper to create data-driven navigation.
 * @class LDK.panel.NavPanel
 *
 * @param {array} sections
 * @param {string} sectionHeaderCls
 * @param {string} secions.header
 * @param {array}secions.items
 * @param {function} renderer Optional. This function will be called on each item in the section.  It should return on Ext config object that wil be added to the panel.  If not provided, a default render will create the row.
 * function(item){
 *      return {
 *          html: '<a href="'+item.url+'">'+item.name+'</a>',
 *          style: 'padding-left:5px;'
 *      }
 *  }
 * @example
    Ext4.create('LDK.panel.NavPanel', {
        renderTo: 'vlDiv',
        width: 350,
        renderer: function(item){
            return {
                html: '<div style="float:left;width:250px;">'+item.name+':</div> [<a href="'+LABKEY.ActionURL.buildURL('query', 'searchPanel', null, {schemaName: item.schemaName, queryName: item.queryName})+'">Search</a>] [<a href="'+LABKEY.ActionURL.buildURL('query', 'executeQuery', null, {schemaName: item.schemaName, 'query.queryName': item.queryName})+'">Browse All</a>]',
                style: 'padding-left:5px;padding-bottom:8px'
            }
        },
        sections: [{
            header: 'Viral Loads',
            items: [
                {name: 'Samples', schemaName: 'laboratory', 'queryName':'Inventory'},
                {name: 'VL Assay Types', schemaName: 'Viral_Load_Assay', 'queryName':'Assays'}
            ]}
        ]
    });

 **/

Ext4.define('LDK.panel.NavPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.ldk-navpanel',

    statics: {
        ITEM_STYLE_DEFAULT: 'padding: 2px;background-color: transparent;',
        ITEM_DEFAULTS: {
            bodyStyle: 'padding: 2px;background-color: transparent;',
            style: 'padding-right: 8px;',
            border: false,
            target: '_self',
            linkCls: 'labkey-text-link'
        }
    },

    initComponent: function(){
        //calculate size
        var maxHeight = this.maxHeight || 15;
        Ext4.QuickTips.init({
            constrainPosition: true
        });

        var size = 0;
        Ext4.each(this.sections, function(i){
            //for the header
            size++;
            size += i.items ? i.items.length : 0;
        }, this);

        var columns = Math.ceil(size / maxHeight);

        Ext4.apply(this, {
            border: false,
            frame: false,
            frameHeader: false,
            minWidth: 300,
            style: 'background-color: transparent;',
            bodyStyle: 'padding:5px;background-color: transparent;',
            width: this.width || '80%',
            defaults: {
                border: false,
                style: 'background-color: transparent;',
                bodyStyle: 'background-color: transparent;'
            }
        });

        this.callParent(arguments);

        var section;
        Ext4.each(this.sections, function(sectionCfg){
            section = this.add({
                xtype: 'panel',
                frame: false,
                width: this.colWidth,
                style: 'padding-right:10px;background-color: transparent;',
                bodyStyle: 'background-color: transparent;border-top-width:0px;',
                cls:   'ldk-navpanel-section',
                defaults: {
                    border: false
                },
                items: [],
                dockedItems: [{
                    dock: 'top',
                    xtype: 'header',
                    cls: this.sectionHeaderCls,
                    title: Ext4.isEmpty(sectionCfg.header) ? '' : sectionCfg.header + ':',
                    hidden: Ext4.isEmpty(sectionCfg.header),
                    style: 'margin-bottom:5px;font-weight:bold;'
                }]
            });

            if (sectionCfg.items){
                for (var j=0;j<sectionCfg.items.length;j++){
                    var item = {};
                    if(sectionCfg.items[j].hasPermission === false)
                        continue;

                    var renderer = null;
                    if(sectionCfg.items[j].renderer)
                        renderer = sectionCfg.items[j].renderer;
                    else if (sectionCfg.renderer)
                        renderer = sectionCfg.renderer;
                    else if(this.renderer)
                        renderer = this.renderer;

                    if(!renderer)
                        renderer = this.renderers.linkWithoutLabel;

                    if(Ext4.isString(renderer))
                        renderer = this.renderers[renderer];

                    item = renderer.call(this, sectionCfg.items[j], sectionCfg);
                    if (!item){
                        continue;
                    }

                    item.cls = 'ldk-navpanel-section-row';

                    section.add(item);
                }
            }

            section.add({tag: 'span', style: 'padding-bottom: 15px;'});
        }, this);
    },

    renderers: {
        linkWithoutLabel: function(item, section){
            if (Ext4.isObject(item.url)){
                item.urlConfig = item.url;
                delete item.url;
            }

            return {
                layout: 'hbox',
                bodyStyle: 'background-color: transparent;',
                defaults: Ext4.apply({}, section.itemDefaults, LDK.panel.NavPanel.ITEM_DEFAULTS),
                items: [{
                    xtype: 'ldk-linkbutton',
                    style: LDK.panel.NavPanel.ITEM_STYLE_DEFAULT + ';margin-left: 2px;',
                    text: item.name,
                    href: item.url ? item.url : item.urlConfig ? LABKEY.ActionURL.buildURL(item.urlConfig.controller, item.urlConfig.action, null, item.urlConfig.params) : null,
                    tooltip: item.tooltip,
                    showBrackets: false
                }]
             }
        },

        linkWithLabel: function(item){
            var cfg = {
                layout: 'hbox',
                bodyStyle: 'background-color: transparent;',
                defaults: LDK.panel.NavPanel.ITEM_DEFAULTS,
                items: [
                    this.getLabelItemCfg(item),
                    {
                        xtype: 'ldk-linkbutton',
                        linkCls: 'labkey-text-link-noarrow',
                        style: LDK.panel.NavPanel.ITEM_STYLE_DEFAULT + ';margin-left: 2px;',
                        text: item.itemText,
                        href: item.url ? item.url : item.urlConfig ? LABKEY.ActionURL.buildURL(item.urlConfig.controller, item.urlConfig.action, null, item.urlConfig.params) : null,
                        tooltip: item.tooltip,
                        showBrackets: false
                    }
                ]
            };

            return cfg;
        },

        linkWithChildren: function(item){
            var cfg = {
                xtype: 'container',
                style: LDK.panel.NavPanel.ITEM_STYLE_DEFAULT,
                items: [this.getLabelItemCfg(item)],
                defaults: {
                    border: false
                },
                hidden: !(item.children && item.children.length)
            };

            if (item.children){
                Ext4.Array.forEach(item.children, function(child) {
                    cfg.items.push({
                        layout: 'hbox',
                        bodyStyle: 'background-color: transparent;',
                        defaults: LDK.panel.NavPanel.ITEM_DEFAULTS,
                        items: [Ext4.apply(this.getLabelItemCfg(child), {style: 'padding: 2px;padding-left: 10px;background-color: transparent;'}), {
                            xtype: 'ldk-linkbutton',
                            linkCls: 'labkey-text-link-noarrow',
                            style: LDK.panel.NavPanel.ITEM_STYLE_DEFAULT,
                            text: child.itemText,
                            href: child.url ? child.url : child.urlConfig ? LABKEY.ActionURL.buildURL(child.urlConfig.controller, child.urlConfig.action, null, child.urlConfig.params) : null,
                            tooltip: child.tooltip,
                            showBrackets: false
                        }]
                    });
                }, this);
            }

            return cfg;
        },

        importingNavItem: function(item){
            if (!item.browseUrl || Ext4.Object.isEmpty(item.browseUrl)){
                return;
            }

            var cfg = {
                layout: 'hbox',
                bodyStyle: 'background-color: transparent;',
                defaults: LDK.panel.NavPanel.ITEM_DEFAULTS,
                items: [
                    this.getLabelItemCfg(item),
                    this.getSearchItemCfg(item, {
                        urlConfig: item.searchUrl
                    }),
                    this.getSpacer(),
                    this.getBrowseItemCfg(item, {
                        urlConfig: item.browseUrl
                    }),
                    this.getSpacer()
                ]
            };

            //if this item supports preparing templates, display a menu with both items.  otherwise, just show import
            if (LABKEY.Security.currentUser.canInsert && !LABKEY.Utils.isEmptyObj(item.importUrl)){
                if (!item.assayRunTemplateUrl || LABKEY.Utils.isEmptyObj(item.assayRunTemplateUrl)){
                    cfg.items.push(this.getImportItemCfg(item, {
                        urlConfig: {
                            params: Ext4.apply({srcURL: LABKEY.ActionURL.buildURL('project', 'begin')}, item.importUrl.params),
                            action: item.importUrl.action,
                            controller: item.importUrl.controller
                        },
                        tooltip: item.importTooltip || 'Click to import data'
                    }));
                }
                else {
                    cfg.items.push({
                        xtype: 'ldk-linkbutton',
                        text: 'Import Data',
                        tooltip: item.importTooltip || 'Click to import data',
                        hidden: !LABKEY.Security.currentUser.canInsert,
                        menu: {
                            xtype: 'menu',
                            plugins: [{
                                ptype: 'menuqtips'
                            }],
                            items: [{
                                text: item.assayRunTemplateText || 'Prepare Run',
                                iconCls: 'x-menu-noicon',
                                qtip: 'Click to upload sample information and prepare an experiment before importing the results.',
                                urlConfig: item.assayRunTemplateUrl,
                                importTitle: item.assayRunTemplateText || 'Import Data',
                                handler: function(btn){
                                    var el = btn.up('button');
                                    Ext4.create('Laboratory.window.WorkbookCreationWindow', {
                                        urlParams: btn.urlConfig.params,
                                        controller: btn.urlConfig.controller,
                                        action: btn.urlConfig.action,
                                        title: btn.importTitle
                                    }).show(el);
                                }
                            },{
                                text: item.viewRunTemplateText || 'View Planned Runs',
                                iconCls: 'x-menu-noicon',
                                qtip: 'Click to view previously saved assay runs.',
                                hidden: !item.viewRunTemplateUrl,
                                href: item.viewRunTemplateUrl ? item.viewRunTemplateUrl.url : undefined
                            },{
                                text: item.uploadResultsText || 'Upload Results',
                                qtip: 'Click to upload new results.',
                                urlConfig: item.importUrl,
                                importTitle: item.uploadResultsText || 'Upload Results',
                                handler: function(btn){
                                    var el = btn.up('button');
                                    Ext4.create('Laboratory.window.WorkbookCreationWindow', {
                                        controller: btn.urlConfig.controller,
                                        action: btn.urlConfig.action,
                                        urlParams: btn.urlConfig.params,
                                        title: btn.importTitle
                                    }).show(el);
                                }
                            }]
                        }
                    });
                }
            }

            return cfg;
        },

        queryRenderer: function(item){
            return {
                layout: 'hbox',
                bodyStyle: 'background-color: transparent;',
                defaults: LDK.panel.NavPanel.ITEM_DEFAULTS,
                items: [
                    this.getLabelItemCfg(item)
                ,
                    this.getSearchItemCfg(item, {
                        url: LABKEY.ActionURL.buildURL("query", "searchPanel", null, {schemaName: item.schemaName, queryName: item.queryName})
                    })
                ,
                    this.getSpacer()
                ,
                    this.getBrowseItemCfg(item, {
                        url: LABKEY.ActionURL.buildURL("query", "executeQuery", null, {schemaName: item.schemaName, 'query.queryName': item.queryName})
                    })
                ,
                    this.getSpacer()
                ,
                    this.getImportItemCfg(item, {
                        urlConfig: {
                            action: 'importData',
                            controller: 'query',
                            params: {schemaName: item.schemaName, queryName: item.queryName, srcURL: LABKEY.ActionURL.buildURL('project', 'begin')}
                        }
                    })
                ]
            };
        },

        workbookRenderer: function(item){
            return {
                layout: 'hbox',
                bodyStyle: 'background-color: transparent;',
                defaults: LDK.panel.NavPanel.ITEM_DEFAULTS,
                items: [
                    this.getLabelItemCfg(item)
                ,
                    this.getSpacer()
                ,
                    this.getBrowseItemCfg(item, {
                        urlConfig: {
                            controller: 'laboratory',
                            action: 'labWorkbooks'
                        },
                        tooltip: 'Click to display a table of all workbooks'
                    })
                ,
                    this.getSpacer()
                ,
                    this.getImportItemCfg(item, {
                        xtype: 'ldk-linkbutton',
                        text: 'Create New Workbook',
                        hidden: !LABKEY.Security.currentUser.canInsert,
                        tooltip: 'Click to create a new workbook',
                        importWizardConfig: {
                            canAddToExistingExperiment: false,
                            title: 'Create Workbook'
                        },
                        target: '_self',
                        urlConfig: {
                            action: 'begin',
                            controller: 'project'
                        }
                    })
                ]
             }
        },

        summaryCountRenderer: function(item){
            var params = {schemaName: item.schemaName, queryName: item.queryName};
            if (item.viewName)
                params['query.viewName'] = item.viewName;

            return {
                layout: 'hbox',
                bodyStyle: 'background-color: transparent;',
                defaults: LDK.panel.NavPanel.ITEM_DEFAULTS,
                items: [
                    this.getLabelItemCfg(item)
                ,{
                    xtype: 'ldk-linkbutton',
                    linkCls: 'labkey-text-link-noarrow',
                    tooltip: 'Click to view these records',
                    href: item.queryName ? LABKEY.ActionURL.buildURL('query', 'executeQuery', null, params): null,
                    text: Ext4.isDefined(item.total) ? item.total.toString() : item.total
                }]
            }
        }
    },

    getSpacer: function(){
        return {
            html: '&nbsp;',
            border: false,
            minWidth: Ext4.isIE7 ? 30 : 0,
            width: Ext4.isIE7 ? 30 : 0
        }
    },

    getSearchItemCfg: function(item, config){
        config = config || {};
        Ext4.apply(config, item);

        return {
            xtype: 'ldk-linkbutton',
            hidden: config.showSearch===false || (!config.url && Ext4.Object.isEmpty(config.urlConfig)),
            tooltip: config.searchTooltip || 'Click to display a search panel',
            href: config.url ? config.url : !Ext4.Object.isEmpty(config.urlConfig) ? LABKEY.ActionURL.buildURL(config.urlConfig.controller, config.urlConfig.action, null, config.urlConfig.params) : null,
            text: 'Search'
        }
    },

    getBrowseItemCfg: function(item, config){
        config = config || {};
        Ext4.apply(config, item);
        return {
            xtype: 'ldk-linkbutton',
            text: config.browseTooltip || 'Browse All',
            tooltip: 'Click to display a table of all records',
            href: config.url ? config.url : !Ext4.Object.isEmpty(config.urlConfig) ? LABKEY.ActionURL.buildURL(config.urlConfig.controller, config.urlConfig.action, null, config.urlConfig.params) : null
        }
    },

    getImportItemCfg: function(item, config){
        config = config || {};
        Ext4.apply(config, item);
        return {
            xtype: 'ldk-linkbutton',
            text: config.text || 'Import Data',
            tooltip: config.importTooltip || 'Click to import new data',
            hidden: config.showImport===false || !LABKEY.Security.currentUser.canInsert,
            importIntoWorkbooks: config.importIntoWorkbooks,
            scope: this,
            handler: function(btn){
                if(LABKEY.Security.currentContainer.isWorkbook || config.importIntoWorkbooks === false)
                    window.location = config.url ? config.url : LABKEY.ActionURL.buildURL(config.urlConfig.controller, config.urlConfig.action, null, config.urlConfig.params);
                else {
                    var wizardCfg = Ext4.apply({
                        controller: config.urlConfig.controller,
                        action: config.urlConfig.action,
                        urlParams: config.urlConfig.params,
                        title: config.importTitle || 'Import Data'
                    }, config.importWizardConfig);
                    Ext4.create('Laboratory.window.WorkbookCreationWindow', wizardCfg).show(btn);
                }
            }
        }
    },

    getLabelItemCfg: function(item, config){
        config = config || {};
        Ext4.apply(config, item);

        var displayLabel = config.label || config.name || config.queryName;
        if (config.showQueryName && displayLabel.toLowerCase() !== config.queryName.toLowerCase()) {
            displayLabel += ' (' + config.queryName + ')'
        }

        return {
            tag: 'div',
            style: LDK.panel.NavPanel.ITEM_STYLE_DEFAULT,
            html: '<span' + (config.description ? ' data-qtip="'+Ext4.htmlEncode(config.description)+'"' : '') + '>' + displayLabel + ':' + '</span>',
            width: config.width || 250
        }
    }
});