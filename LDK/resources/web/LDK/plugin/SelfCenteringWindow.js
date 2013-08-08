/**
 * A plugin the will cause an Ext window to automatically center itself within the screen
 * after resize
 *
 * @cfg constrainToWindow
 */
Ext4.define('LDK.plugin.SelfCenteringWindow', {
    extend: 'Ext.AbstractPlugin',
    pluginId: 'ldk-selfcenteringwindow',
    mixins: {
        observable: 'Ext.util.Observable'
    },

    alias: 'plugin.ldk-selfcenteringwindow',

    init: function(window){
        this.window = window;

        window.on('resize', function(window){
            //move the window if it extends off screen
            var position = window.getPosition();
            if ((position[1] + window.getHeight()) > Ext4.getBody().getHeight()){
                var y = (Ext4.getBody().getHeight() - window.getHeight()) / 2;
                window.setPosition(position[0], y);
            }
        }, window);

        if (this.constrainToWindow){
            window.autoScroll = true;
            window.maxHeight = this.getMaxHeightValue();
            Ext4.EventManager.onWindowResize(this.onWindowResize, this);
        }
    },

    onWindowResize: function(){
        this.window.maxHeight = this.getMaxHeightValue();
        this.window.doLayout();
    },

    getMaxHeightValue: function(){
        return Ext4.getBody().getViewSize().height * 0.9;
    }
});
