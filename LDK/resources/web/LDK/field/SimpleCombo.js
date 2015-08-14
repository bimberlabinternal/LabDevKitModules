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
    }
});
