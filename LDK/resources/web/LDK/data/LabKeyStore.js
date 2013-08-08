/**
 * An extension to LABKEY.ext4.Store, which provides a case-insensitive model
 */
Ext4.define('LDK.data.LabKeyStore', {
    extend: 'LABKEY.ext4.Store',
    alias: 'widget.ldk-labkeystore',

    constructor: function(){
        var me = this;

        me.fields = me.fields || me.fields;
        if (!me.model) {

            me.model = Ext4.define('Ext.data.Store.ImplicitModel-' + (me.storeId || Ext4.id()), {
                extend: 'LDK.data.CaseInsensitiveModel',
                fields: me.fields,
                proxy: me.proxy || me.defaultProxyType
            });

            delete me.fields;

            me.implicitModel = true;
        }

        this.callParent(arguments);
    }
});