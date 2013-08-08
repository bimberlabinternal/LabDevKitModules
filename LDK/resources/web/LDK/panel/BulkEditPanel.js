/**
 * @cfg dataRegionName
 */
Ext4.define('LDK.panel.BulkEditPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.ldk-bulkeditpanel',

    initComponent: function(){
        Ext4.apply(this, {
            defaults: {
                border: false
            },
            items: [{
                html: 'Loading...'
            }]
        });

        this.callParent();

        this.loadRows();
    },

    loadRows: function(){
        var dr = this.getDataRegion();
        if (!dr){
            Ext4.Msg.alert('Error', 'Unable to find the DataRegion');
            return;
        }

        var checked = dr.getChecked();
        if (!checked.length){
            Ext4.Msg.alert('Error', 'No rows selected');
            return;
        }

        var config = LDK.QueryHelper.getQueryConfigFromDataRegion(dr);
        var qh = Ext4.create('LDK.QueryHelper', config);
        qh.appendFilter(LABKEY.Filter.create('', checked.join(';'), LABKEY.Filter.Types.EQUALS_ONE_OF));
        this.store = qh.createStore();
        this.store.on('load', this.onStoreLoad, this, {single: true});
        this.store.load();
    },

    getDataRegion: function(){
        return LABKEY.DataRegions[this.dataRegionName];
    },

    onStoreLoad: function(store){
        console.log('load');
    },

    getItems: function(){
        var toAdd = [];





        this.removeAll();
        if (toAdd.length)
            this.add(toAdd);
    }
});

Ext4.define('LDK.window.BulkEditWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.ldk-bulkeditwin',

    initComponent: function(){
        Ext4.apply(this, {
            title: 'Bulk Edit',
            modal: true,
            closeAction: 'destroy',
            defaults: {
                border: false
            },
            items: [{
                xtype: 'ldk-bulkeditpanel',
                itemId: 'bulkEditPanel',
                dataRegionName: this.dataRegionName
            }],
            buttons: [{
                text: 'Submit',
                scope: this,
                handler: function(btn){
                    this.down('#bulkEditPanel').onSubmit();
                }
            },{
                text: 'Cancel',
                handler: function(btn){
                    btn.up('window').close();
                }
            }]
        });

        this.callParent();
    }
});