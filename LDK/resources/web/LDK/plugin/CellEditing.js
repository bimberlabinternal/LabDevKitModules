Ext4.define('LDK.grid.plugin.CellEditing', {
    extend: 'Ext.grid.plugin.CellEditing',

    onSpecialKey: function(ed, field, e) {
        var grid = this.grid, sm;
        if (e.getKey() === e.TAB) {
            e.stopEvent();
            sm = grid.getSelectionModel();
            if (sm.onEditorTab)sm.onEditorTab(this, e);
        }
        else if (e.getKey() === e.ENTER){
            e.stopEvent();
            sm = grid.getSelectionModel();
            if (sm.onEditorEnter)
                sm.onEditorEnter(this, e);
        }
    },

    getEditingContext: function(record, columnHeader){
        //NOTE: this exists to prevent editing on calculated columns or others not bound to a field
        if (!columnHeader || !columnHeader.dataIndex){
            return null;
        }

        return this.callParent(arguments);
    }
});