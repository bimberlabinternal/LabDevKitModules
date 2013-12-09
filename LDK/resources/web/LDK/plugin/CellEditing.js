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

    //NOTE: this is an override to fix an Ext4 bug involving  revertInvalid
    getEditor: function(record, column) {
        var editor = this.callParent(arguments);
        Ext4.apply(editor, {
            revertInvalid: false,
            completeEdit : function(remainVisible) {
                var me = this,
                        field = me.field,
                        value;

                if (!me.editing) {
                    return;
                }

                // Assert combo values first
                if (field.assertValue) {
                    field.assertValue();
                }

                value = me.getValue();
                if (!field.isValid()) {
                    if (me.revertInvalid !== false) {
                        me.cancelEdit(remainVisible);
                        return;
                    }
                }

                if (String(value) === String(me.startValue) && me.ignoreNoChange) {
                    me.hideEdit(remainVisible);
                    return;
                }

                if (me.fireEvent('beforecomplete', me, value, me.startValue) !== false) {
                    // Grab the value again, may have changed in beforecomplete
                    value = me.getValue();
                    if (me.updateEl && me.boundEl) {
                        me.boundEl.update(value);
                    }
                    me.hideEdit(remainVisible);
                    me.fireEvent('complete', me, value, me.startValue);
                }
            }
        });

        return editor;
    },

    startEdit: function(record, columnHeader){
        //NOTE: this was added to allow editors to be conditionally created, based on the data of that row.  See ClinicalObservations in EHR
        this.editors.clear();
        this.callParent(arguments);
    },

    getEditingContext: function(record, columnHeader){
        //NOTE: this exists to prevent editing on calculated columns or others not bound to a field
        if (Ext4.isObject(columnHeader) && !columnHeader.dataIndex){
            return null;
        }

        return this.callParent(arguments);
    }
});