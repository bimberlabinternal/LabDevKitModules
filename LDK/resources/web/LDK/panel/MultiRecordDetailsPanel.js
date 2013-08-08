/**
 * This panel is designed to provide DetailsView of records in situations where there may be more than 1 record.  When this occurs, it can either
 * render a series of LDK.panel.DetailsPanels (one per record in the store), or render the results in a single QWP
 * @class LABKEY.panel.MultiRecordDetailsPanel
 * @cfg store {Ext.data.Store/Object} A store or store config
 * @cfg {string} titleField
 * @cfg {string} titlePrefix Defaults to 'Details'
 * @cfg {object} qwpConfig
 * @cgf {object} detailsConfig
 * @cfg {boolean} multiToGrid
 */
Ext4.define('LDK.panel.MultiRecordDetailsPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.ldk-multirecorddetailspanel',
    initComponent: function(){
        Ext4.apply(this, {
            minHeight: 50,
            border: false,
            defaults: {
                border: false
            }
        });

        this.callParent();
        this.showLoadMask();

        this.store = this.getStore();
        this.mon(this.store, 'load', this.onStoreLoad, this);
        if (!this.store.getCount() || this.store.isLoading)
            this.store.load();
        else
            this.onStoreLoad();
    },

    showLoadMask: function(){
        this.loadMask = this.loadMask || new Ext4.LoadMask(this, {msg:"Loading..."});

        if (this.rendered){
            this.loadMask.show();
        }
        else {
            this.on('afterrender', this.showLoadMask, this, {single: true, delay: 100});
        }
    },

    getStore: function(){
        if(!this.store.events){
            this.store.autoLoad = false;
            this.store = Ext4.create('LABKEY.ext4.Store', this.store);
        }

        return this.store;
    },

    onStoreLoad: function(){
        this.removeAll();
        this.loadMask.hide();

        if (!this.store.getCount()){
            this.add({
                xtype: 'ldk-webpartpanel',
                title: this.titlePrefix,
                html: 'No records found'
            });
            return;
        }

        if (this.store.getCount() > 1 && this.multiToGrid){
            var config = this.store.getQueryConfig();
            Ext4.applyIf(config, {
                allowChooseQuery: false,
                allowChooseView: true,
                showInsertNewButton: false,
                showDeleteButton: false,
                showDetailsColumn: true,
                showUpdateColumn: false,
                showRecordSelectors: true,
                buttonBarPosition: 'top',
                title: this.titlePrefix,
                timeout: 0
            });

            if(this.qwpConfig){
                Ext4.apply(config, this.qwpConfig);
            }

            this.add({
                xtype: 'ldk-querypanel',
                queryConfig: config
            });
        }
        else {
            var toAdd = [];
            this.store.each(function(rec, idx){
                toAdd.push(this.getDetailsPanelCfg(rec, idx));
            }, this);

            this.add(toAdd);
        }
    },

    getDetailsPanelCfg: function(rec, idx){
        var title = this.titlePrefix || 'Details';
        if (this.titleField && rec.get(this.titleField)){
            title += ': ' + rec.get(this.titleField);
        }

        return {
            xtype: 'ldk-detailspanel',
            store: this.store,
            boundRecord: rec,
            title: title,
            style: idx > 0 ? 'padding-top: 10px' : null,
            detailsConfig: this.detailsConfig
        };
    }
});