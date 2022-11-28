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
    joinReturnValue: false,
    delimiter: ';',
    forceSelection: true,
    typeAhead: true,
    queryMode: 'local',
    triggerAction: 'all',

    initComponent: function(){
        Ext4.apply(this, {
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

        if (this.initialValues) {
            if (!Ext4.isArray(this.initialValues)) {
                this.initialValues = this.initialValues.split(';');
            }

            this.setValue(this.initialValues);
        }
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

            // wait for store load:
            return;
        }

        if (this.multiSelect && val && Ext4.isString(val)) {
            val = val.split(this.delimiter);
        }

        if (this.store && this.valueField){
            var field = this.store.getFields().get(this.valueField);
            if (field){
                if (Ext4.isPrimitive(val)) {
                    val = field.convert(val);
                    if (Ext4.isDefined(val)) {
                        arguments[0] = val;
                    }
                }
                else if (Ext4.isArray(val)) {
                    Ext4.Array.forEach(val, function(v, idx){
                        if (Ext4.isPrimitive(v)) {
                            v = field.convert(v);
                            if (Ext4.isDefined(v)) {
                                val[idx] = v;
                            }
                        }
                    }, this);
                }
            }
        }

        this.callParent(arguments);
    },

    getSubmitValue: function(){
        var val = this.callParent(arguments);
        if (!Ext4.isArray(val) || !this.joinReturnValue) {
            return val;
        }

        return val && val.length ? val.join(this.delimiter) : null;
    },

    getToolParameterValue : function(){
        return this.getSubmitValue();
    }
});
