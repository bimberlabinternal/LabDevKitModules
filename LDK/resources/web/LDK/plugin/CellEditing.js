/**
 * This was created primarily to allow more keyboard navigation, such as allowing enter to
 * move between rows
 */
Ext4.define('LDK.grid.plugin.CellEditing', {
    extend: 'Ext.grid.plugin.CellEditing',

    onCellClick: function(view, cell, colIdx, record, row, rowIdx, e) {
        //NOTE: if holding shift or ctrl, dont allow editing ot start
        if (e && (e.shiftKey || e.ctrlKey)){
            return;
        }

        this.callParent(arguments);
    },

    onSpecialKey: function(ed, field, e) {
        if (e.getKey() === e.ENTER){
            var grid = this.grid, sm;
            e.stopEvent();
            sm = grid.getSelectionModel();

            if (sm.onEditorEnter)
                return sm.onEditorEnter(ed.editingPlugin, e);
        }

        return this.callParent(arguments);
    },

    getEditor: function(record, column) {
        if (column && column.editable === false){
            return false;
        }

        var editor = this.callParent(arguments);

        //NOTE: these are reused, so dont override multiple times
        if (editor && !editor.hasCellEditOverrides){
            Ext4.apply(editor, {
                hasCellEditOverrides: true,
                revertInvalid: false,
                completeOnEnter: false,
                alignment: 'tl-tl?'
            });

            //NOTE: this is an override to fix an Ext4 bug involving revertInvalid
            Ext4.override(editor, {
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
                            return;   //NOTE: this is the changed line
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
        }

        return editor;
    },

    getEditingContext: function(record, columnHeader){
        //NOTE: this exists to prevent editing on calculated columns or others not bound to a field
        if (Ext4.isObject(columnHeader) && !columnHeader.dataIndex){
            return null;
        }

        return this.callParent(arguments);
    }
});