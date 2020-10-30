Ext4.ns('LDK.DataRegionUtils');

LDK.DataRegionUtils = new function(){


    return {
        bulkEditHandler: function(dataRegionName, btn){
            Ext4.create('LDK.window.BulkEditWindow', {
                dataRegionName: dataRegionName
            }).show(btn);
        },

        getDataRegionWhereClause: function(dataRegion, tableAlias, success, onNoneSelected, failure){
            var selectorCols = !Ext4.isEmpty(dataRegion.selectorCols) ? dataRegion.selectorCols : dataRegion.pkCols;
            LDK.Assert.assertNotEmpty('Unable to find selector columns for: ' + dataRegion.schemaName + '.' + dataRegion.queryName, selectorCols);
            LDK.Assert.assertTrue(success + ' is not a valid success function.', Ext4.isFunction(success));

            var selectionSuccess = function(selection) {
                dataRegion.clearSelected({
                    success: function () {
                        if (!selection.selected.length && Ext4.isFunction(onNoneSelected)) {
                            onNoneSelected();
                        }
                        else {
                            var colExpr = '(' + tableAlias + '.' + selectorCols.join(" || ',' || " + tableAlias + ".") + ')';
                            var clause = "WHERE " + colExpr + " IN ('" + selection.selected.join("', '") + "')";
                            success(clause);
                        }
                    },
                    failure: Ext4.isFunction(failure) ? failure : LDK.Utils.getErrorCallback()
                });
            };

            var config = {
                success: selectionSuccess,
                failure: Ext4.isFunction(failure)?failure:LDK.Utils.getErrorCallback()
            };

            dataRegion.getSelected(config);
        },

        getDisplayName: function(dataRegion){
            return dataRegion.schemaName + '.' + dataRegion.queryName;
        }
    }
};