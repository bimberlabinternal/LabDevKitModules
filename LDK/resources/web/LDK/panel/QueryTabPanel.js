Ext4.define("LDK.panel.QueryTabPanel", {
    alias: 'widget.ldk-querytabpanel',
    extend: 'Ext.tab.Panel',

    initComponent: function(){
        this.callParent(arguments);

        if (!Ext4.isIE){
            this.on('tabchange', this.onTabChange, this);
        }

        this.on('afterrender', function(panel){
            this.originalWidth = Math.max(this.getWidth(), Ext4.getBody().getWidth());
        }, this);
    },

    onTabChange: function(tabPanel, newCard, oldCard){
        var qwps = [];
        var children = newCard.query('ldk-contentresizingpanel');
        if (children.length){
            qwps = qwps.concat(children);
        }

        if (newCard.isXType('ldk-contentresizingpanel')){
            qwps.push(newCard);
        }

        if (qwps.length){
            Ext4.Array.forEach(qwps, function(q){
                q.onContentSizeChange()
            }, this);
        }
    },

    doResize: function(itemWidth){
        var width2 = this.getWidth();
        if (itemWidth > width2){
            this.setWidth(itemWidth);
            this.doLayout();
        }
        else if (itemWidth < width2) {
            if (this.originalWidth && width2 != this.originalWidth){
                this.setWidth(Math.max(this.originalWidth, itemWidth));
                this.doLayout();
            }
        }
    }
});