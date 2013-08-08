/**
 * This is an extension of the LABKEY DetailsPanel which renders using border designed to match a webpart
 * @param detailsConfig A config object that will be applied to the LABKEY.ext.DetailsPanel
 */
Ext4.define('LDK.panel.DetailsPanel', {
    extend: 'LDK.panel.WebpartPanel',
    alias: 'widget.ldk-detailspanel',

    initComponent: function(){
        var detailsConfig = Ext4.apply({
            xtype: 'labkey-detailspanel',
            showTitle: false,
            border: false,
            addFieldsForRecord: function(rec){
                var toAdd = this.getDetailItems(rec);

                if (this.columns){
                    var itemsPerCol = toAdd.length / this.columns;
                    itemsPerCol = Math.ceil(itemsPerCol);
                    var items = [];
                    for (var i=0;i<this.columns;i++){
                        var newItems = toAdd.splice(0, Math.min(itemsPerCol, toAdd.length));

                        items.push({
                            //xtype: 'column',
                            border: false,
                            defaults: {
                                border: false
                            },
                            items: newItems
                        });
                    }

                    toAdd = items;
                }
                this.removeAll();
                this.add(toAdd);
            }
        }, this.detailsConfig);

        if (detailsConfig.columns){
            detailsConfig.layout = 'column';
        }
        Ext4.each(['store', 'showBackBtn'], function(prop){
            if (Ext4.isDefined(this[prop])){
                detailsConfig[prop] = this[prop];
                this[prop] = null;
            }
        }, this);

        this.items = [detailsConfig];
        this.callParent(arguments);
    }
});