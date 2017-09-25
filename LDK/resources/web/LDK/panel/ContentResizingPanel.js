/**
 * This is designed to help with the problem of rendering dynamic content into Ext4 panels.  It's a little ugly, but this
 * panel provides a div into which you render the webpart or report.  On load, it will listen for that element's DOMSubtreeModified
 * event, and resize the Ext containers if needed.  It attempts to batch these events and only actually trigger a layout when a resize
 * is needed.
 *
 * NOTE: this currently handles the problem by adding DOMSubtreeModified listeners and manually resizing on change.
 * A more elegant solution would be to dig into Ext's layout engine and make a custom layout that will
 * auto-size itself based on the width of our target element.  We'd need to override the methods that calculate weight and height.
 * Figuring that out will make WebPartPanel work better too.
 */
Ext4.define('LDK.panel.ContentResizingPanel', {
    extend: 'Ext.panel.Panel', //note: extending panel is required
    alias: 'widget.ldk-contentresizingpanel',
    divPrefix: 'contentPanel',

    initComponent: function(){
        this.renderTarget = this.divPrefix + '-' + Ext4.id();

        Ext4.apply(this, {
            border: false,
            html: '<div class="ldk-wp" id="'+this.renderTarget+'"></div>'
        });

        this.callParent(arguments);

        this.addEvents('contentsizechange');
        this.on('contentsizechange', this.onContentSizeChange, this, {buffer: 200});
        this.on('afterrender', this.onAfterPanelRender, this);
    },

    onAfterPanelRender: function() {
        // Room for horizontal scrollbar
        if(Ext4.isDefined(this.overflowX)) {
            this.setHeight(this.getHeight() + 5);
        }
    },

    onContentSizeChange: function(){
        var el = Ext4.get(this.renderTarget);
        var size = el.getSize();
        var mySize = this.previousSize || this.body.getSize();
        if (mySize.height != size.height || mySize.width != size.width){
            this.setSize(size);
            this.previousSize = size;
        }
    },

    createListeners: function(isRetry){
        this.doLayout();

        var el = Ext4.get(this.renderTarget);
        if (!Ext4.isDefined(el) && !isRetry){
            Ext4.defer(this.createListeners, 250, this, [true]);
            return;
        }

        if (Ext4.isIE){
            var panel = this;

            el = el.query('*[name="webpart"]')[0];
            if (!Ext4.isDefined(el) && !isRetry){
                Ext4.defer(this.createListeners, 250, this, [true]);
                return;
            }

            if (!el && isRetry) {
                return;
            }

            if (el.addEventListener){
                el.addEventListener('DOMSubtreeModified', function(){
                    panel.fireEvent('contentsizechange');
                }, false);
            }
            else if (el.attachEvent){
                el.attachEvent('DOMSubtreeModified', function(){
                    panel.fireEvent('contentsizechange');
                });
            }
            else {
                LDK.Utils.logToServer({
                    message: 'Unable to find appropriate event in ContentResizingPanel',
                    level: 'ERROR',
                    includeContext: true
                });
            }
        }
        else if (Ext4.isDefined(el)) {
            el.on('DOMSubtreeModified', function(){
                this.fireEvent('contentsizechange');
            }, this);

            // Issue 31454: if the output has a clickable labkey-wp-header, also listen for that event
            Ext4.each(el.query('.labkey-wp-header'), function(wpHeader) {
                Ext4.get(wpHeader).on('click', function(){
                    this.fireEvent('contentsizechange');
                }, this);
            }, this);
        }
    },

    getWidth: function() {
        var el = Ext4.get(this.renderTarget);
        if (Ext4.isDefined(el)) {
            return el.getWidth();
        }
        return this.callParent();
    }
});