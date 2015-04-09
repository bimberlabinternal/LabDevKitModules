Ext4.define('LDK.field.ExpDataField', {
    extend: 'Ext.form.field.Trigger',
    alias: 'widget.ldk-expdatafield',

    triggerCls: 'x4-form-search-trigger',

    initComponent: function() {
        this.callParent(arguments);

    },

    onRender : function(ct, position){
        this.callParent(arguments);

        this.wrap = this.inputEl.wrap({
            tag: 'div',
            cls: 'x4-form-field-wrap'
        });

        this.fileDiv = this.wrap.createChild({
            tag: 'div',
            style: 'vertical-align:top;'
        });

    },

    onTriggerClick: function(){
        var val = this.getValue();
        if (!val){
            this.setText(null);
            return;
        }

        LABKEY.Query.selectRows({
            containerFilter: 'WorkbookAssay',
            containerPath: Laboratory.Utils.getQueryContainerPath(),
            schemaName: 'exp',
            queryName: 'data',
            filterArray: [LABKEY.Filter.create('rowid', val)],
            failure: LDK.Utils.getErrorCallback(),
            columns: 'RowId,Name,Extension',
            success: function(results){
                if (!results.rows.length){
                    Ext4.Msg.alert('Error', 'Data with Id: ' + val + ' not found');
                    this.setValue(null);
                }
                else {
                    this.setText(results.rows[0].Name);
                }
            },
            scope: this
        });
    },

    setText: function(text){
        this.fileDiv.update(text);
    },

    onDestroy : function() {
        if (this.fileDiv){
            this.fileDiv.removeAllListeners();
            this.fileDiv.remove();
        }

        if (this.wrap){
            this.wrap.remove();
        }

        this.callParent(this);
    }
});