/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('LDK.plugin.UserEditableCombo', {
    extend: 'Ext.AbstractPlugin',
    pluginId: 'ldk-usereditablecombo',
    mixins: {
        observable: 'Ext.util.Observable'
    },

    alias: 'plugin.ldk-usereditablecombo',
    allowChooseOther: true,

    init: function(combo) {
        this.combo = combo;
        //NOTE: if this is true, when we have a zero record store we always select the 'other' record, triggering a popup
        combo.selectOnTab = false;
        combo.userEditablePlugin = this;

        Ext4.override(combo, {
            onListSelectionChange: function(list, selectedRecords) {
                var val;
                if(selectedRecords && selectedRecords.length && selectedRecords.length === 1)
                    val = selectedRecords[0].get(this.displayField);

                if (this.userEditablePlugin.allowChooseOther && val === 'Other'){
                    this.getPicker().getSelectionModel().deselectAll(true); //note: we need to clear selection in case other is clicked twice in a row
                    this.userEditablePlugin.onClickOther();
                    this.collapse();
                }
                else
                {
                    this.callOverridden(arguments);
                }
            },

            ensureValueInStore: function(){
                this.addValueIfNeeded(this.getValue());
            },

            addValueIfNeeded: function(val){
                if (Ext4.isEmpty(val))
                    return;

                if (val instanceof Ext4.data.Model || Ext4.isArray(val)){
                    return;  //if setting value using a record, it is probably in the store
                }

                if (!this.store){
                    LDK.Utils.logToServer({
                        message: 'Unable to find store in usereditable combo'
                    });
                    return;
                }

                if (this.store.isLoading()){
                    this.store.on('load', function(){
                        this.addValueIfNeeded(val);
                    }, this, {single: true});
                    return;
                }

                //only applies if display/value are the same.  otherwise we cannot accurately do this
                if (this.valueField !== this.displayField) {
                    var type1 = this.store.model.fields.get(this.valueField) && this.store.model.fields.get(this.displayField).type ? this.store.model.fields.get(this.valueField).type.type : null;
                    var type2 = this.store.model.fields.get(this.displayField) && this.store.model.fields.get(this.displayField).type ? this.store.model.fields.get(this.displayField).type.type : null;

                    if (type1 !== 'string' || type2 !== 'string') {
                        return;
                    }
                }

                if (Ext4.isObject(val)){
                    console.log(val);
                    return;
                }

                var recIdx = this.store.findExact(this.valueField, val);
                if (recIdx !== -1)
                    return;

                var rec = this.store.createModel({});
                rec.set(this.valueField, val);
                rec.set(this.displayField, val);

                this.store.add(rec);
            },

            setValue: function(val){
                this.addValueIfNeeded(val);

                this.callOverridden(arguments);
            }
        });

        combo.store.on('add', this.onStoreAdd, this);
        combo.store.on('load', combo.ensureValueInStore, combo);
        combo.store.on('beforerender', combo.ensureValueInStore, combo);

        if (this.allowChooseOther){
            if (LABKEY.ext4.Util.hasStoreLoaded(combo.store)){
                this.addOtherRecord();
            }
            else {
                if (!combo.store.on)
                    combo.store = Ext4.ComponentMgr.create(combo.store);
            }

            combo.store.on('load', function(){
                this.addOtherRecord();
            }, this);
        }
    },

    customRecords: {},

    destroy: function(){
        this.isDestroyed = true;
    },

    onStoreAdd: function(store, recs){
        Ext4.Array.forEach(recs, function(r){
            var pk = r.get(this.combo.valueField);
            if (!this.customRecords[pk]){
                this.customRecords[pk] = r;
            }
        }, this);
    },

    addOtherRecord: function(){
        if (this.isDestroyed){
            return;
        }

        var rec = this.combo.findRecord(this.combo.displayField, 'Other');
        if (rec){
            return;
        }

        var data = {};
        data[this.combo.valueField] = 'Other';
        data[this.combo.displayField] = 'Other';
        this.addRecord(data, this.combo.store.getCount());
    },

    onClickOther: function(){
        if (this.isDestroyed){
            return;
        }

        this.addEditorListeners();
        this.createWindow();
    },

    createWindow: function(){
        Ext4.MessageBox.prompt('Enter Value', 'Enter value:', this.onPrompt, this);
    },

    onPrompt: function(btn, val){
        if (btn === 'ok') {
            this.addNewValue(val);
        }

        var editor = this.combo.up('editor');
        if (editor){
            this.mun(editor, 'beforecomplete', this.onBeforeComplete, this);
            this.combo.getPicker().refresh();
            this.combo.expand();
            Ext4.defer(this.combo.focus, 20, this.combo);
        }
        else {
            this.combo.fireEvent('blur', this.combo);
        }
    },

    addEditorListeners: function(){
        var editor = this.combo.up('editor');
        if (editor){
            this.mon(editor, 'beforecomplete', this.onBeforeComplete, this);
        }
    },

    onBeforeComplete: function(){
        return !Ext4.Msg.isVisible();
    },

    addNewValue: function(val){
        var data = {};

        if (Ext4.isObject(val)){
            data = val;
        }
        else {
            data[this.combo.valueField] = val;
            data[this.combo.displayField] = val;
        }


        this.combo.setValue(this.addRecord(data));
    },

    addRecord: function(data, idx){
        if (!data || LABKEY.Utils.isEmptyObj(data)) {
            return;
        }

        if (this.isDestroyed){
            return;
        }

        if (!this.combo.store || !LABKEY.ext4.Util.hasStoreLoaded(this.combo.store)){
            this.combo.store.on('load', function(store){
                this.addRecord(data, idx);
            }, this, {single: true});

            console.error('unable to add record: ' + this.combo.store.storeId);
            console.log(data);

            return;
        }

        idx = idx || (this.combo.store.getCount() === 0 ? 0 : this.combo.store.getCount() - 1);
        this.combo.store.insert(idx, [this.combo.store.createModel(data)]);

        return this.combo.store.getAt(idx);
    }
});