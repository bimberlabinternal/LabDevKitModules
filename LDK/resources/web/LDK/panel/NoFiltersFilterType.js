Ext4.define('LDK.panel.NoFiltersFilterType', {
    extend: 'LDK.panel.AbstractFilterType',
    alias: 'widget.ldk-nofiltersfiltertype',

    statics: {
        filterName: 'none',
        label: 'Entire Database'
    },

    initComponent: function(){
        this.callParent();
    },

    getFilters: function(){
        return null;
    },

    getTitle: function(){
        return '';
    }
});