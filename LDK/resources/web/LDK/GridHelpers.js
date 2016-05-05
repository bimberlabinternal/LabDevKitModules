/**
 * A panel that provides UI to duplicate records from a grid.
 * @param targetStore The store where the new records will be added
 * @param records The records to duplicate
 */
Ext4.define('LDK.ext.RecordDuplicatorWin', {
    extend: 'Ext.window.Window',
    alias: 'widget.ldk-recordduplicatorwin',

    initComponent: function(){
        Ext4.apply(this, {
            closeAction: 'destroy',
            modal: true,
            title: 'Duplicate Records',
            items: [{
                xtype: 'form',
                width: 350,
                border: true,
                bodyStyle: 'padding: 5px',
                defaults: {
                    width: 200,
                    border: false
                },
                items: [{
                    xtype: 'numberfield',
                    fieldLabel: 'Number of Copies',
                    itemId: 'newRecs',
                    value: 1
                },{
                    xtype: 'fieldset',
                    fieldLabel: 'Choose Fields to Copy',
                    items: this.getItems()
                }]
            }],
            buttons: [{
                text:'Submit',
                disabled:false,
                itemId: 'submit',
                scope: this,
                handler: function(btn){
                    var win = btn.up('window');
                    win.duplicate();
                    win.close();
                }
            },{
                text: 'Close',
                handler: function(btn){
                    btn.up('window').close();
                }
            }]
        });

        this.callParent(arguments);
    },

    getItems: function(){
        var toAdd = [];
        this.targetStore.getFields().each(function(f){
            if (!f.hidden && f.shownInInsertView && f.allowDuplicateValue!==false){
                toAdd.push({
                    xtype: 'checkbox',
                    dataIndex: f.dataIndex,
                    name: f.dataIndex,
                    fieldLabel: f.fieldLabel,
                    checked: !f.noDuplicateByDefault
                });
            }
        }, this);
        return toAdd;
    },

    duplicate: function(){
        Ext4.each(this.records, function(rec){
            var toAdd = [];
            var idx = this.targetStore.indexOf(rec);
            idx++;

            for (var i=0;i<this.down('#newRecs').getValue();i++){
                var data = {};
                this.down('form').getForm().getFields().each(function(f){
                    if(f.checked){
                        data[f.dataIndex] = rec.get(f.dataIndex);
                    }
                }, this);
                toAdd.push(LDK.StoreUtils.createModelInstance(this.targetStore, data, true));
            }
            this.targetStore.insert(idx, toAdd);
        }, this);
    }
});


/**
 * A panel that provides generic UI to sort a store client-side, based on multiple fields.  It was written to be used
 * with a grid button
 * @param targetGrid The store where the new records will be added
 */
Ext4.define('LDK.ext.StoreSorterWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.ldk-storesorterwindow',

    initComponent: function(){
        var storeData = [];
        this.targetGrid.store.getFields().each(function(field){
            if(!field.isHidden)
                storeData.push([field.name, field.fieldLabel]);
        }, this);

        this.sortFields = Ext4.create('Ext.data.ArrayStore', {
            fields: [
                'name',
                'label'
            ],
            idIndex: 0,
            data: storeData
        });

        Ext4.apply(this, {
            modal: true,
            items: [{
                itemId: 'theForm',
                border: true,
                bodyStyle: 'padding:5px',
                defaults: {
                    width: 400,
                    border: false,
                    bodyBorder: false
                },
                items: [{
                    xtype: 'combo',
                    emptyText: '',
                    fieldLabel: 'Sort 1',
                    displayField: 'label',
                    valueField: 'name',
                    typeAhead: true,
                    editable: true,
                    queryMode: 'local',
                    store: this.sortFields,
                    itemId: 'sortField'
                },{
                    xtype: 'combo',
                    emptyText: '',
                    fieldLabel: 'Sort 2',
                    displayField: 'label',
                    valueField: 'name',
                    typeAhead: true,
                    editable: true,
                    queryMode: 'local',
                    store: this.sortFields,
                    itemId: 'sortField2'
                },{
                    xtype: 'combo',
                    emptyText: '',
                    fieldLabel: 'Sort 3',
                    displayField: 'label',
                    valueField: 'name',
                    typeAhead: true,
                    editable: true,
                    queryMode: 'local',
                    store: this.sortFields,
                    itemId: 'sortField3'
                }]
            }],
            buttons: [{
                text:'Submit',
                disabled:false,
                itemId: 'submit',
                scope: this,
                handler: this.doSort
            },{
                text: 'Close',
                scope: this,
                handler: function(btn){
                    btn.up('window').hide();
                }
            }]
        });

        this.callParent(arguments);
    },

    doSort: function(){
        var field1 = this.down('#sortField').getValue();
        var field2 = this.down('#sortField2').getValue();
        var field3 = this.down('#sortField3').getValue();

        if(!field1){
            alert('Must pick a field');
            return;
        }

        var fields = [];
        var stores = [];
        Ext4.each([field1, field2, field3], function(fn){
            if(fn){
                var meta = this.targetGrid.store.getFields().get(fn);
                fields.push(meta);

                //pre-create store if it does not exist
                if (meta.lookup){
                    var store = Ext4.StoreMgr.get(LABKEY.ext4.Util.getLookupStoreId(meta));
                    if (!store){
                        store = LABKEY.ext4.Util.getLookupStore(meta);
                    }
                    stores.push(store);
                }
            }
        }, this);

        Ext4.Msg.wait('Sorting');
        this.sortGrid(fields, stores);
    },

    sortGrid: function(fields, stores){
        if (this.storesReady(stores)){
            //avoid remoteSort problems
            this.targetGrid.store.data.sortBy(LDK.StoreUtils.getStoreSortFn(fields));
            this.targetGrid.getView().refresh();
            this.targetGrid.store.fireEvent('datachanged', this.targetGrid.store);

            Ext4.Msg.hide();
            this.close();
        }
        else {
            this.sortGrid.defer(100, this, [fields, stores]);
        }
    },

    storesReady: function(stores){
        var ready = true;
        Ext4.each(stores, function(store){
            if (store.isLoading())
                ready = false;
        }, this);
        return ready;
    }
});



Ext4.define('LDK.ext.BatchEditWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.ldk-batcheditwindow',

    initComponent: function(){
        var sm = this.targetGrid.getSelectionModel();
        this.selectedRecords = sm.getSelection();

        Ext4.apply(this, {
            closeAction: 'hide',
            modal: true,
            title: 'Bulk Edit',
            items: [{
                xtype: 'form',
                bodyStyle: 'padding:5px',
                defaults: {
                    border: false
                },
                items: [{
                    html: 'Editing ' + this.selectedRecords.length + ' records',
                    style: 'padding-bottom: 10px;background-color: transparent;'
                },{
                    emptyText: '',
                    fieldLabel: 'Select Field',
                    itemId: 'fieldName',
                    xtype: 'combo',
                    displayField: 'name',
                    valueField: 'value',
                    typeAhead: true,
                    queryMode: 'local',
                    width: 400,
                    isFormField: false,
                    editable: false,
                    required: true,
                    store: this.getFieldStore(),
                    listeners: {
                        scope: this,
                        select: function(combo, recs){
                            var rec = recs[0];
                            var editor = this.targetGrid.store.getFormEditorConfig(rec.get('value'));
                            editor.width = 400;
                            if(editor.originalConfig.inputType=='textarea')
                                editor.height = 100;
                                editor.itemId = editor.name;

                            if(!this.down('#' + editor.name)){
                                this.down('form').add(editor);
                            }
                            combo.reset();
                        }
                    }
                }]
            }],
            buttons: [{
                text:'Reset',
                disabled:false,
                itemId: 'reset',
                scope: this,
                handler: function(){
                    var form = this.down('form');
                    form.getForm().getFields().each(function(item){
                        if(item.isFormField){
                            form.remove(item);
                        }
                    }, this);
                }
            },{
                text:'Submit',
                disabled:false,
                formBind: true,
                itemId: 'submit',
                scope: this,
                handler: this.onEdit
            },{
                text: 'Close',
                handler: function(btn){
                    btn.up('window').close();
                }
            }]
        });

        this.callParent(arguments);
    },

    getFieldStore: function(){
        var fields = [];
        var map = LDK.StoreUtils.getFieldMap(this.targetGrid.store);
        fields = [];
        Ext4.each(this.targetGrid.columns, function(col){
            if (!col.hidden && !col.isReadOnly && col.userEditable !== false){
                var meta = map[col.dataIndex];
                fields.push([meta.dataIndex, meta.fieldLabel, meta]);
            }
        }, this);

        return Ext4.create('Ext.data.ArrayStore', {
            fields: ['value', 'name', 'meta'],
            data: fields
        });
    },

    onEdit: function (){
        var toChange = {};
        this.down('form').getForm().getFields().each(function(item){
            //TODO: centralize how we capture these values
            if(item.isFormField){
                var v;
                if (item instanceof Ext4.form.RadioGroup){
                    v = (item.getValue() ? item.getValue().inputValue : null);
                }
                else if (item instanceof Ext4.form.Radio){
                    if(item.checked)
                        v = item.getValue();
                    else
                        v = false;
                }
                else if (item instanceof Ext4.form.CheckboxGroup){
                    v = item.getValueAsString();
                }
                else
                    v = item.getValue();

                toChange[item.name] = v;
            }
        }, this);

        Ext4.each(this.selectedRecords, function(r){
            r.set(toChange);
        }, this);

        this.close();
    }
});


Ext4.define('LDK.ext.SpreadsheetImportWindow', {
    extend: 'Ext.window.Window',

    initComponent: function(){
        Ext4.apply(this, {
            modal: true,
            title: 'Spreadsheet Import',
            width: 620,
            items: [{
                xtype: 'form',
                bodyStyle: 'padding: 5px;',
                defaults: {
                    border: false
                },
                items: [{
                    html: 'This allows you to upload records to this grid using an excel template.  Click the link below to download the template.',
                    style: 'padding-bottom: 10px;'
                },{
                    xtype: 'ldk-linkbutton',
                    text: 'Download Template',
                    linkPrefix: '[',
                    linkSuffix: ']',
                    style: 'padding-bottom: 10px;',
                    handler: function(btn){
                        var win = btn.up('window');
                        var fields;
                        if (win.includeVisibleColumnsOnly){
                            var map = LDK.StoreUtils.getFieldMap(win.targetGrid.store);
                            fields = [];
                            Ext4.each(win.targetGrid.columns, function(col){
                                if (!col.hidden){
                                    fields.push(map[col.dataIndex]);
                                }
                            }, this);
                        }
                        else {
                            fields = win.targetGrid.store.model.getFields()
                        }

                        LDK.StoreUtils.createExcelTemplate({
                            fields: fields,
                            skippedFields: [],
                            fileName: win.fileNamePrefix + '_' + Ext4.Date.format(new Date(), 'Y-m-d H_i_s') + '.xls'
                        });
                    }
                },{
                    xtype: 'textarea',
                    itemId: 'textField',
                    width: 600,
                    height: 300
                }]
            }],
            buttons: [{
                text: 'Submit',
                scope: this,
                handler: function(btn){
                    var win = btn.up('window');
                    var text = win.down('#textField').getValue();

                    if (!text){
                        Ext4.Msg.alert('Must provide text');
                        return;
                    }

                    LDK.StoreUtils.addTSVToStore({
                        store: win.targetGrid.store,
                        text: text
                    });

                    win.close();
                }
            },{
                text: 'Cancel',
                handler: function(btn){
                    btn.up('window').close();
                }
            }]
        });

        this.callParent(arguments);
    }
});
