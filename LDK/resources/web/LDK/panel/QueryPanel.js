/**
 * Experimental.  This is designed to simplify the process of rendering a QWP in an Ext4 panel, since the panel will not normally
 * resize itself to accommodate the QWP.  It also defers loading the QWP until after the panel is rendered, which can be convenient since
 * the QWP will fail unless the target element is actually present.  See LDK.panel.ContentResizingPanel for additional information.
 *
 * @cfg queryConfig A config object passed directly to create the QWP
 */
Ext4.define('LDK.panel.QueryPanel', {
    extend: 'LDK.panel.ContentResizingPanel',
    alias: 'widget.ldk-querypanel',
    divPrefix: 'queryPanel',

    initComponent: function(){
        Ext4.apply(this, {
            minHeight: 20,
            listeners: {
                scope: this,
                afterrender: this.loadQuery
            }
        });

        this.callParent(arguments);
    },

    loadQuery: function(){
        var panel = this;
        if(panel.qwp){
            return;
        }

        var qwpConfig = Ext4.apply({
            showDetailsColumn: false,
            linkTarget: '_blank',
            failure: LDK.Utils.getErrorCallback()
        }, panel.queryConfig);

        Ext4.apply(qwpConfig, {
            renderTo: this.renderTarget
        });

        var success = qwpConfig.success;
        var scope = qwpConfig.scope;
        qwpConfig.success = Ext4.Function.pass(function(callback, scope, dr, response){
            var width = Ext4.get('dataregion_'+dr.id).getSize().width + 60;
            var existingWidth = this.getWidth();
            if (width > existingWidth){
                this.setWidth(width);
            }

            if (callback)
                callback.apply(scope || this, [dr, response]);
        }, [success, scope], this);
        qwpConfig.scope = null;


        if (panel.rendered)
            panel.createListeners();
        else
            panel.on('afterrender', panel.createListeners, panel, {single: true});

        panel.qwp = LDK.Utils.getBasicQWP(qwpConfig);
    }
});
