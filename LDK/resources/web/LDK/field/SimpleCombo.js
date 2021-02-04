/**
 * An extension of the Ext4 combo that can be instantiated using a simple list of allowable values.
 * Example: Ext4.widget({xtype: 'ldk-simplecombo', storeValues: 'foo;bar', fieldLabel: 'Test'});
 *
 * @cfg storeValues Either an array or semicolon delimited list of the values for the drop down
 */
Ext4.define('LDK.form.field.SimpleCombo', {
    extend: 'LABKEY.ext4.ComboBox',
    alias: 'widget.ldk-simplecombo',

    forceSelection: true,
    typeAhead: true,
    queryMode: 'local',
    triggerAction: 'all',
    joinReturnValue: false,
    delimiter: ';',

    initComponent: function(){
        Ext4.apply(this, {
            displayField: 'value',
            valueField: 'value',
            store: {
                type: 'array',
                fields: ['value'],
                data: this.parseStoreValues()
            }
        });

        this.callParent(arguments);

        if (this.initialValues) {
            if (!Ext4.isArray(this.initialValues)) {
                this.initialValues = this.initialValues.split(';');
            }

            this.setValue(this.initialValues);
        }
    },

    parseStoreValues: function(){
        this.storeValues = this.storeValues || [];
        if (Ext4.isString(this.storeValues)){
            this.storeValues = this.storeValues.split(';');
        }

        var vals = [];
        Ext4.Array.forEach(this.storeValues, function(val){
            vals.push([val]);
        }, this);
        vals = Ext4.unique(vals);

        return vals;
    },

    getSubmitValue: function(){
        var val = this.callParent(arguments);
        if (!this.joinReturnValue) {
            return val;
        }

        return val && val.length ? val.join(this.delimiter) : null;
    },

    getToolParameterValue : function(){
        return this.getSubmitValue();
    },

    setValue: function(val) {
        if (this.multiSelect && val && Ext4.isString(val)) {
            val = val.split(this.delimiter);
            this.callParent([val]);
        }
        else {
            this.callParent(arguments);
        }
    }
});
