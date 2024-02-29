Ext4.define('Laboratory.field.PedigreeSelectorField', {
    extend: 'LABKEY.ext4.ComboBox',
    alias: 'widget.laboratory-pedigreeselectorfield',

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
                fields: ['value']
            }
        });

        this.callParent(arguments);

        this.loadData();
    },

    loadData: function(){
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('laboratory', 'getDemographicsProviders', Laboratory.Utils.getQueryContainerPath()),
            method : 'POST',
            scope: this,
            failure: LDK.Utils.getErrorCallback(),
            success: LABKEY.Utils.getCallbackWrapper(function(response){
                console.log(response);
                Ext4.Array.forEach(response.providers, function(d){
                    if (d.isValidForPedigree) {
                        this.store.add(this.store.createModel({value: d.label}));
                    }
                }, this);
            }, this)
        });
    },

    getToolParameterValue : function(){
        return this.getSubmitValue();
    }
});
