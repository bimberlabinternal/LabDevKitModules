/**
 * @cfg containerPath
 * @cfg schemaName
 * @cfg queryName
 * @cfg filterArray
 * @cfg sortField
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
                filterArray: this.filterArray,
                sort: this.sortField,
                autoLoad: true
            }
        });

        this.callParent(arguments);
    },

    setValue: function(val){
        if (this.store && this.store.isLoading()){
            var args = arguments;
            this.store.on('load', function(){
                if (this.isDestroyed || (this.store && this.store.isDestroyed)){
                    return;
                }

                this.setValue.apply(this, args);
            }, this, {defer: 100, single: true});
        }

        if (this.store && this.valueField && Ext4.isPrimitive(val)){
            var field = this.store.getFields().get(this.valueField);
            if (field){
                val = field.convert(val);
                if (Ext4.isDefined(val)) {
                    arguments[0] = val;
                }
            }
        }

        this.callParent(arguments);
    }
});
