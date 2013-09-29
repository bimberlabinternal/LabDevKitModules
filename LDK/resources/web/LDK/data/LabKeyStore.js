/**
 * An extension to LABKEY.ext4.data.Store, which provides a case-insensitive model
 */
Ext4.define('LDK.data.LabKeyStore', {
    extend: 'LABKEY.ext4.data.Store',
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
    },

    getProxyConfig: function(){
        var cfg = this.callParent();
        cfg.type = 'LDKAjaxProxy';

        return cfg;
    }
});

Ext4.define('LDK.data.proxy.AjaxProxy', {
    extend: 'LABKEY.ext4.data.AjaxProxy',
    alias: 'proxy.LDKAjaxProxy',

    reader: 'LDKExtendedJsonReader'
});

Ext4.define('LDK.data.proxy.ExtendedJsonReader', {
    extend: 'LABKEY.ext4.data.JsonReader',
    alias: 'reader.LDKExtendedJsonReader',

    readRecords: function(data) {
        if (data.metaData){
            Ext4.each(data.metaData.fields, function(field){
                if (!field.name){
                    var fk = new LABKEY.FieldKey.fromParts(field.fieldKey);
                    field.name = fk.toString();
                }
            }, this);
        }

        return this.callParent([data]);
    },

    //NOTE: this has been overridden b/c 13.2 style records have the data placed in a node named 'data', rather than top-level
    extractData : function(root) {
        var me = this,
                records = [],
                Model   = me.model,
                length  = root.length,
                convertedValues, node, record, i;

        if (!root.length && Ext4.isObject(root)) {
            root = [root];
            length = 1;
        }

        for (i = 0; i < length; i++) {
            node = root[i];

            //NOTE: this line changes compared to Ext
            record = new Model(undefined, me.getId(node), node.data, convertedValues = {});
            record.phantom = false;

            //NOTE: this line changes compared to Ext
            me.convertRecordData(convertedValues, node.data, record);

            records.push(record);

            if (me.implicitIncludes) {
                me.readAssociated(record, node);
            }
        }

        return records;
    }
});