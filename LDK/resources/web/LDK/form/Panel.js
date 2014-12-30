Ext4.define('LDK.form.Panel', {
    extend: 'LABKEY.ext4.FormPanel',
    alias: 'widget.ldk-formpanel',
    defaultFieldWidth: 450,
    defaultFieldLabelWidth: 180,

    configureForm: function(store){
        var items = this.callParent(arguments);
        this.walkItems(items);

        return items;
    },

    walkItems: function(items){
        Ext4.Array.forEach(items, function(item){
            if (item.xtype == 'combo' || item.xtype == 'labkey-combo'){
                item.plugins = item.plugins || [];
                item.plugins.push(Ext4.create('LDK.plugin.UserEditableCombo', {
                    allowChooseOther: false
                }));
            }

            if (item.xtype == 'numberfield'){
                item.hideTrigger = true;
            }
        }, this);

        if (items.items)
            this.walkItems(items.items);
    }
});