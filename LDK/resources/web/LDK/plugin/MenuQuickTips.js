/**
 * A plugin which allows menu items to have tooltips.
 */
Ext4.define('LDK.plugin.MenuQuickTips', {
    alias: 'plugin.menuqtips',
    init: function (menu) {
        menu.items.each(function (item) {
            if (typeof (item.qtip) != 'undefined')
                item.on('afterrender', function (menuItem) {
                    var qtip = typeof (menuItem.qtip) == 'string'
                            ? {text: menuItem.qtip}
                            : menuItem.qtip;
                    qtip = Ext4.apply(qtip, {target: menuItem.getEl().getAttribute('id')});
                    Ext4.QuickTips.register(qtip);
                });
        });
    }
});
