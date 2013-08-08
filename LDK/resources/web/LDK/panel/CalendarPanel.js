Ext4.define('LDK.panel.CalendarPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.ldk-calendarpanel',

    initComponent: function(){
        if (!this.store.events){
            console.log('creating store');
            this.store = Ext4.ComponentManager.create(this.store);
        }

        Ext4.apply(this, {
            title: 'Calendar Panel',
            calendarMap: {}
        });

        this.store.on('load', this.onStoreLoad);
        this.callParent();
    },

    onStoreLoad: function(store){

    },

    getCalendarConfig: function(){

    },

    getCalendarForRecord: function(rec){

    }
})