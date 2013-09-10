Ext4.ns('LDK.DataRegionUtils');

LDK.DataRegionUtils = new function(){


    return {
        bulkEditHandler: function(dataRegionName, btn){
            Ext4.create('LDK.window.BulkEditWindow', {
                dataRegionName: dataRegionName
            }).show(btn);
        },

        getDataRegionWhereClause: function(dataRegion, tableAlias){
            var selectorCols = !Ext4.isEmpty(dataRegion.selectorCols) ? dataRegion.selectorCols : dataRegion.pkCols;
            LDK.Assert.assertNotEmpty('Unable to find selector columns for: ' + dataRegion.schemaName + '.' + dataRegion.queryName, selectorCols);

            var colExpr = '(' + tableAlias + '.' + selectorCols.join(" || ',' || " + tableAlias + ".") + ')';
            return "WHERE " + colExpr + " IN ('" + dataRegion.getChecked().join("', '") + "')";
        },

        getDisplayName: function(dataRegion){
            return dataRegion.schemaName + '.' + dataRegion.queryName;
        }
    }
};