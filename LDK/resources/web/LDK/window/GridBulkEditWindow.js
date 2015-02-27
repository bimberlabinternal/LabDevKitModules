/**
 * @cfg targetGrid
 */
Ext4.define('LDK.window.GridBulkEditWindow', {
    extend: 'Ext.window.Window',

    initComponent: function() {
        Ext4.apply(this, {
            width: 500,
            bodyStyle: 'padding: 5px;',
            closeAction: 'destroy',
            modal: true,
            title: 'Bulk Edit',
            items: [{
                xtype: 'form',
                defaults: {
                    width: 375
                },
                border: false,
                items: [{
                    html: 'This helper allows you to bulk edit the selected rows.  To use it, first pick the desired fields using the drop-down below.  Selecting a field will add the editor for that field.  You can repeat this as many times as you want in order to change multiple fields at once.  When finished, hit submit to apply your changes.',
                    border: false,
                    width: null,
                    style: 'padding-bottom: 10px;'
                },{
                    emptyText: '',
                    fieldLabel: 'Select Field',
                    itemId: 'fieldName',
                    xtype: 'checkcombo',
                    multiSelect: true,
                    displayField: 'name',
                    valueField: 'value',
                    typeAhead: true,
                    triggerAction: 'all',
                    queryMode: 'local',
                    editable: false,
                    store: this.getFieldStore(),
                    isFormField: false,
                    listeners: {
                        scope: this,
                        change: function(field, values, oldVal){
                            if (values && values.length) {
                                Ext4.Array.forEach(values, function(val){
                                    var idx = field.store.find('value', val);
                                    if (idx > -1) {
                                        var rec = field.store.getAt(idx);
                                        this.addEditor(rec.get('column'));
                                    }
                                }, this);

                                var form = this.down('form');
                                form.items.each(function(field){
                                    if (field.dataIndex){
                                        if (values.indexOf(field.name) == -1){
                                            form.remove(field);
                                        }
                                    }
                                })
                            }
                        }
                    }
                }]
            }],
            buttons: [{
                text:'Submit',
                disabled:false,
                formBind: true,
                itemId: 'submit',
                scope: this,
                handler: this.onBulkEdit
            },{
                text: 'Close',
                itemId: 'close',
                scope: this,
                handler: function(btn){
                    this.close();
                }
            }]
        });

        this.callParent(arguments);
    },

    onBulkEdit: function(btn){
        var s = this.targetGrid.getSelectionModel().getSelection();
        if (!s.length){
            Ext4.Msg.alert('Error', 'No rows selected');
        }

        var vals = this.down('form').getForm().getFieldValues();
        for (var i=0;i<s.length;i++){
            s[i].set(vals);
        }

        this.close();
    },

    getFieldStore: function(){
        var values = [];
        Ext4.each(this.targetGrid.columns, function (c) {
            if (!c.hidden) {
                values.push([c.dataIndex, c.text, c]);
            }
        }, this);

        return Ext4.create('Ext.data.ArrayStore', {
            fields: ['value', 'name', 'column'],
            data: values
        });
    },

    addEditor: function(column){
        if (column && column.getEditor){
            var ed = column.getEditor();
            if (ed) {
                if (this.down('field[name=' + column.dataIndex + ']')){
                    return;
                }

                var field = ed.cloneConfig({
                    fieldLabel: column.text,
                    name: column.dataIndex,
                    dataIndex: column.dataIndex
                });

                delete field.width;
                this.down('form').add(field);
            }
        }
    }
});