/**
 * A simple component that will render a simple hyperlink, supporting a few features
 * click click handlers, custom menus, tooltips, etc.
 * @cfg text
 * @cfg menu
 * @cfg tooltip
 * @cfg handler
 * @cfg scope
 * @cfg linkPrefix
 * @cfg linkSuffix
 * @cfg linkCls
 * @cfg linkTarget
 */
Ext4.define('LDK.button.LinkButton', {
    extend: 'Ext.Component',
    alias: 'widget.ldk-linkbutton',

    renderTpl: [
        '<span id="{id}-wrap">',
        '<tpl if="linkPrefix">{linkPrefix}</tpl>',
        '<a id="{id}-linkEl "',
        '<tpl if="href"> href="{href} " </tpl>',
        '<tpl if="linkTarget"> target="{linkTarget}" </tpl>',
        '<tpl if="tooltip"> data-qtip="{tooltip}"</tpl>' +
        '<tpl if="linkCls"> class="{linkCls}"</tpl>' +
        '>{text}</a>',
        '<tpl if="linkSuffix">{linkSuffix}</tpl>',
        '</span>'
    ],
    renderSelectors: {
        linkEl: 'a'
    },

    initComponent: function() {
        if (this.menu) {
            // retrieve menu by id or instantiate instance if needed
            this.menu = Ext4.menu.Manager.get(this.menu);

            // Use ownerButton as the upward link. Menus *must have no ownerCt* - they are global floaters.
            // Upward navigation is done using the up() method.
            this.menu.ownerButton = this;
        }

        this.renderData = {
            id: this.id,
            href: this.href,
            text: this.text,
            baseCls: this.baseCls,
            linkTarget: this.linkTarget,
            linkPrefix: this.linkPrefix,
            linkSuffix: this.linkSuffix,
            linkCls: this.linkCls,
            tooltip: this.tooltip
        };

        this.callParent();
    },

    // @private
    getRefItems: function(deep){
        var items;

        if (this.menu) {
            items = this.menu.getRefItems(deep);
            items.unshift(this.menu);
        }
        return items || [];
    },

    // @private
    onClick: function(e) {
        if (this.preventDefault || (this.disabled && this.getHref()) && e) {
            e.preventDefault();
        }

        if (!this.disabled) {
            //me.doToggle();
            this.maybeShowMenu();
            this.fireHandler(e);
        }
    },

    maybeShowMenu: function(){
        if (this.menu && !this.hasVisibleMenu() && !this.ignoreNextClick) {
            this.showMenu(true);
        }
    },

    showMenu: function(/* private */ fromEvent) {
        var me = this, menu = me.menu;

        if (me.rendered) {
            if (me.tooltip && Ext4.quickTipsActive && me.getTipAttr() != 'title') {
                Ext4.tip.QuickTipManager.getQuickTip().cancelShow(me.el);
            }

            if (menu.isVisible()) {
                menu.hide();
            }

            if (!fromEvent || me.showEmptyMenu || menu.items.getCount() > 0) {
                menu.showBy(me.el, me.menuAlign);
            }
        }
        return me;
    },

    hideMenu: function() {
        if (this.hasVisibleMenu()) {
            this.menu.hide();
        }
        return this;
    },

    /**
     * Returns true if the button has a menu and it is visible
     * @return {Boolean}
     */
    hasVisibleMenu: function() {
        var menu = this.menu;
        return menu && menu.rendered && menu.isVisible();
    },

    fireHandler: function(e) {
        if (this.fireEvent('click', this, e) !== false) {
            if (this.handler) {
                this.handler.call(this.scope || this, this, e);
            }
        }
    },

    // @private
    onRender: function() {
        this.callParent(arguments);

        // Add whatever button listeners we need
        this.mon(this.el, 'click', this.onClick, this);
    }
});
