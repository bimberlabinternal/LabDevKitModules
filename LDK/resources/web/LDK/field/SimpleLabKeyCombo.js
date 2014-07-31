/**
 * @cfg schemaName
 * @cfg queryName
 */
Ext4.define('LDK.form.field.SimpleLabKeyCombo', {
    extend: 'LABKEY.ext4.ComboBox',
    alias: 'widget.ldk-simplelabkeycombo',

    initComponent: function(){
        Ext4.apply(this, {
            forceSelection: true,
            typeAhead: true,
            queryMode: 'local',
            triggerAction: 'all',
            store: {
                type: 'labkey-store',
                containerPath: this.containerPath,
                schemaName: this.schemaName,
                queryName: this.queryName,
                autoLoad: true
            }
        });

        this.callParent(arguments);
    }
});
