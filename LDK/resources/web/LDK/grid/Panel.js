/**
 * This is an extension of the LABKEY gridpanel, primarily extended to more easily allow changes without altering
 * the core.
 */
Ext4.define('LDK.grid.Panel', {
    extend: 'LABKEY.ext4.GridPanel',
    alias: 'widget.ldk-gridpanel',

    initComponent: function(){
        Ext4.apply(this, {
            cls: 'ldk-grid'
        });

        this.callParent(arguments);

        //this.on('afterlayout', this.setupTextWrapping, null, {delay: 200});
    },

    getEditingPlugin: function(){
        var plugin = this.callParent(arguments);
        if (plugin){
            Ext4.override(plugin, {
                getEditingContext: function(record, columnHeader){
                    if (!columnHeader || !columnHeader.dataIndex){
                        return null;
                    }

                    return this.callOverridden(arguments);
                }
            });
        }
        return plugin;
    },

    /**
     * This adds CSS to enable text wrapping, but only on non-hidden columns
     * Hidden columns are still rendered on the page, except with 0 width, so their
     * text wraps abnormally if allowed.
     */
    setupTextWrapping: function(grid){
        var cls = 'ldk-wrap-text';
        if (grid.columns.length){
            Ext4.each(grid.columns, function(column){
                var id = column.getId();
                var cells = Ext4.DomQuery.select('.x4-grid-cell-'+id);
                column.addCls(cls);
                column.cls = cls;

                for (var i = 0; i < cells.length; i++){
                    if (column.hidden === true) {
                        Ext4.fly(cells[i]).removeCls();
                    }
                    else {
                        Ext4.fly(cells[i]).addCls(cls);
                    }
                }
            });
        }
    },

    onMenuCreate: function(headerCt, menu){
        menu.items.each(function(item){
            if (item.text == 'Columns'){
                console.log('remove');
                menu.remove(item);
            }
        }, this);

        console.log(arguments);
    }

//    onColumnHide: function(ct, column){
//        console.log('hide');
//        console.log(column);
//        column.tdCls = null;
//    },
//
//    onColumnShow: function(ct, column){
//        console.log('show');
//        column.tdCls = 'newClass';
//        console.log(column);
//
//    }
});

Ext4.apply(LABKEY.ext4.GRIDBUTTONS, {
    //@Override
    ADDRECORD: function(config){
        return Ext4.Object.merge({
            text: 'Add Record',
            tooltip: 'Click to add a row',
            handler: function(btn){
                var grid = btn.up('gridpanel');
                if(!grid.store)
                    return;

                var cellEditing = grid.getPlugin(grid.editingPluginId);
                if(cellEditing)
                    cellEditing.completeEdit();

                var model = LDK.StoreUtils.createModelInstance(grid.store, null, true);
                grid.store.insert(0, [model]); //add a blank record in the first position

                if(cellEditing)
                    cellEditing.startEditByPosition({row: 0, column: this.firstEditableColumn || 0});
            }
        }, config);
    },
    /**
     * @param config.fileNamePrefix
     * @param config.includeVisibleColumnsOnly
     */
    SPREADSHEETADD: function(config){
        return Ext4.Object.merge({
            text: 'Add From Spreadsheet',
            tooltip: 'Click to upload data using an excel template, or download this template',
            handler: function(btn){
                var grid = btn.up('gridpanel');
                Ext4.create('LDK.ext.SpreadsheetImportWindow', {
                    targetGrid: grid,
                    includeVisibleColumnsOnly: config.includeVisibleColumnsOnly,
                    fileNamePrefix: config.fileNamePrefix
                }).show(btn);
            }
        }, config);
    },

    DUPLICATE: function(config){
        return Ext4.Object.merge({
            text: 'Duplicate Selected',
            tooltip: 'Duplicate Selected Records',
            handler: function(btn){
                var grid = btn.up('gridpanel');
                var records = grid.getSelectionModel().getSelection();
                if(!records || !records.length){
                    Ext4.Msg.alert('Error', 'No rows selected');
                    return;
                }

                Ext4.create('LDK.ext.RecordDuplicatorWin', {
                    targetStore: grid.store,
                    records: records
                }).show(btn);
            }
        });
    },

    SORT: function(config){
        return Ext4.Object.merge({
            text: 'Sort',
            tooltip: 'Click to sort the records',
            handler: function(btn){
                var grid = btn.up('gridpanel');

                Ext4.create('LDK.ext.StoreSorterWindow', {
                    targetGrid: grid
                }).show(btn);
            }
        }, config);
    },

    BULKEDIT: function(config){
        return Ext4.Object.merge({
            text: 'Bulk Edit',
            disabled: false,
            tooltip: 'Click this to change values on all checked rows in bulk',
            scope: this,
            handler : function(btn){
                var grid = btn.up('gridpanel');

                var totalRecs = grid.getSelectionModel().getSelection().length;
                if(!totalRecs){
                    Ext4.Msg.alert('Error', 'No rows selected');
                    return;
                }

                Ext4.create('LDK.ext.BatchEditWindow', {
                    targetGrid: grid
                }).show(btn);
            }
        });
    },

    DELETERECORD: function(config){
        return Ext4.Object.merge({
            text: 'Delete Records',
            tooltip: 'Click to delete selected rows',
            handler: function(btn){
                var grid = btn.up('gridpanel');
                var start = new Date();
                var selections = grid.getSelectionModel().getSelection();
                if(!grid.store || !selections || !selections.length)
                    return;

                LDK.StoreUtils.bulkRemove(grid.store, selections);
                grid.getView().refresh();
            }
        }, config);
    }
});