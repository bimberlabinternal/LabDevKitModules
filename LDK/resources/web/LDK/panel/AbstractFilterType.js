Ext4.define('LDK.panel.AbstractFilterType', {
    extend: 'Ext.panel.Panel',

    // This is passed to the filter types to apply a non-removable filter on these QC state labels
    reportQCStates: [],

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
        let filterArray = {
            removable: [],
            nonRemovable: []
        };

        if (this.reportQCStates?.length) {
            filterArray.nonRemovable.push(LABKEY.Filter.create('qcstate/label', this.reportQCStates, LABKEY.Filter.Types.EQUALS_ONE_OF));
        }

        return filterArray;
    },

    getTitle: function(){
        alert('Error: FilterType should implement getTitle()');
    },

    validateReportForFilterType: function(report){
        return null;  //subclasses should implement this
    },

    prepareRemove: Ext4.emptyFn
});