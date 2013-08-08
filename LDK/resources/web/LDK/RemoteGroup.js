Ext4.define('LDK.ext4.RemoteGroup', {
    initGroup: function(field){
        Ext4.apply(this, {
            name: this.name || Ext4.id(),
            //layout: 'checkboxgroup',
            //autoHeight: true,
            //storeLoaded: false,
            items: [{
                name: 'placeholder',
                fieldLabel: 'Loading...'
            }],
            fieldRendererTpl : new Ext4.XTemplate('<tpl for=".">' +
                    '{[values["' + this.valueField + '"] ? values["' + this.displayField + '"] : "'+ (this.nullCaption ? this.nullCaption : '[none]') +'"]}' +
                //allow a flag to display both display and value fields
                    '<tpl if="'+this.showValueInList+'">{[(values["' + this.valueField + '"] ? " ("+values["' + this.valueField + '"]+")" : "")]}</tpl>'+
                    '</tpl>'
            ).compile()
        });

        //we need to test whether the store has been created
        if(!this.store && this.queryName)
            this.store = LABKEY.ext.Ext4Helper.simpleLookupStore(this);

        if(!this.store){
            console.log('RemoteGroup requires a store');
            return;
        }

        if(this.store && !this.store.events)
            this.store = Ext4.create(this.store, 'labkey-store');

        if(!this.store || !this.store.model || !this.store.model.prototype.fields.getCount())
            this.mon(this.store, 'load', this.onStoreLoad, this, {single: true});
        else
            this.onStoreLoad();
    }

    ,onStoreLoad : function(store, records, success) {
        this.removeAll();
        if(!success){
            this.add('Error Loading Store');
        }
        else {
            var toAdd = [];
            var config;
            this.store.each(function(record, idx){
                config = {
                    boxLabel: (this.tpl ? this.fieldRendererTpl.apply(record.data) : record.get(this.displayField)),
                    inputValue: record.get(this.valueField),
                    disabled: this.disabled,
                    readOnly: this.readOnly || false
                };

                if(this instanceof Ext4.form.RadioGroup)
                    config.name = this.name+'_radio';

                toAdd.push(config);
            }, this);
            this.add(toAdd);
        }
    }
});


/* options:
 valueField: the inputValue of the checkbox
 displayField: the label of the checkbox
 store
 nullCaption
 showValueInList

 */
Ext4.define('LDK.ext4.RemoteCheckboxGroup', {
    extend: 'Ext.form.CheckboxGroup',
    alias: 'widget.ldk-remotecheckboxgroup',
    initComponent: function(){
        this.initGroup(this);
        this.callParent(arguments);
    },
    mixins: {
        remotegroup: 'LDK.ext4.RemoteGroup'
    }
});


/* options:
 valueField: the inputValue of the checkbox
 displayField: the label of the checkbox
 store
 nullCaption
 showValueInList

 */
Ext4.define('LDK.ext4.RemoteRadioGroup', {
    extend: 'Ext.form.RadioGroup',
    alias: 'widget.ldk-remoteradiogroup',
    initComponent: function(){
        this.initGroup(this);
        this.callParent(arguments);
    },
    mixins: {
        remotegroup: 'LDK.ext4.RemoteGroup'
    }
});