Ext4.ns('LDK.DataRegionUtils');

LDK.DataRegionUtils = new function(){


    return {
        bulkEditHandler: function(dataRegionName, btn){
            Ext4.create('LDK.window.BulkEditWindow', {
                dataRegionName: dataRegionName
            }).show(btn);
        }
    }
};