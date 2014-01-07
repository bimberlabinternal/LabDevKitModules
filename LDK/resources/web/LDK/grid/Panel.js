/**
 * This is an extension of the LABKEY gridpanel, primarily extended to more easily allow changes without altering
 * the core.
 */
Ext4.define('LDK.grid.Panel', {
    extend: 'LABKEY.ext4.GridPanel',
    alias: 'widget.ldk-gridpanel',

    selType: 'ldk-rowmodel',

    initComponent: function(){
        Ext4.apply(this, {
            cls: 'ldk-grid'
        });

        this.callParent(arguments);

    },

    onMenuCreate: function(headerCt, menu){
        menu.items.each(function(item){
            if (item.text == 'Columns'){
                menu.remove(item);
            }
        }, this);
    },

    getEditingPlugin: function(){
        return Ext4.create('LDK.grid.plugin.CellEditing', {
            pluginId: this.editingPluginId,
            clicksToEdit: this.clicksToEdit
        });
    }
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

//adapted from: http://stackoverflow.com/questions/10179047/extjs-4-excel-style-keyboard-navigation-in-an-editable-grid
Ext4.define('LDK.grid.panel.RowSelectionModel', {
    extend: 'Ext.selection.RowModel',
    alias: 'selection.ldk-rowmodel',

    lastId:null,

    onEditorEnter: function(ep,e){
        var me = this,
                view = me.view,
                record = ep.getActiveRecord(),
                header = ep.getActiveColumn(),
                position = view.getPosition(record, header),
                direction = e.shiftKey ? 'up' : 'down',
                newPosition = view.walkCells(position, direction, e, false),
                newId = newPosition.row,
                grid = view.up('gridpanel');

        //NOTE: if the editor is a combo and it is expanded, defer to the combo's handling
        if (ep.activeEditor && ep.activeEditor.field && ep.activeEditor.field.isExpanded){
            return;
        }

        var deltaY = 20 * (direction == 'down' ? 1 : -1);
        grid.scrollByDeltaY(deltaY);
        me.lastId = newPosition.row;
        if (newPosition)
            ep.startEditByPosition(newPosition);
        else
            ep.completeEdit();
    }
});