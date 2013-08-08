Ext4.ns('LDK.AssayUtils');

/**
 * A static helper with methods designed to assist with LabKey assays
 */
LDK.AssayUtils = new function(){

    return {
        SCHEMA_NAME: 'assay',

        /**
         * @param assayName
         * @return {String} The name of the query containing batch records
         */
        getBatchQueryName: function(assayName){
            return assayName + ' Batches';
        },

        /**
         * @param assayName
         * @return {String} The name of the query containing run records
         */
        getRunQueryName: function(assayName){
            return assayName + ' Runs';
        },

        /**
         * @param assayName
         * @return {String} The name of the query containing the results
         */
        getResultQueryName: function(assayName){
            return assayName + ' Data';
        }
    }
}