Ext4.define('LDK.panel.AbstractFilterType', {
    extend: 'Ext.panel.Panel',

    initComponent: function(){
        Ext4.apply(this, {
            layout: 'hbox',
            border: false,
            defaults: {
                border: false
            }
        });

        this.callParent();
    },

    initFilters: function(){
        this.removeAll();
        var toAdd = this.getItems();
        if (toAdd && toAdd.length)
            this.add(toAdd);
    },

    isValid: function(){
        return true;
    },

    getFilterInvalidMessage: function(){
        if (!this.isValid()){
            console.error('subclasses must implement getFilterInvalidMessage()');
        }
    },

    getFilterArray: function(tab, subject){
        return {
            removable: [],
            nonRemovable: []
        };
    },

    getTitle: function(){
        alert('Error: FilterType should implement getTitle()');
    },

    validateReportForFilterType: function(report){
        return null;  //subclasses should implement this
    },

    prepareRemove: Ext4.emptyFn
});