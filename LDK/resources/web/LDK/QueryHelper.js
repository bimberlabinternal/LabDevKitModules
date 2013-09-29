/**
 * EXPERIMENTAL.  The goal of this class is to encapsulate the tricky parts of dealing with query parameters and
 * to expose a more consistent API.  It could be used directly, and will also be used internally within
 * other classes like a store
 *
 */
Ext4.define('LDK.QueryHelper', {
    mixins: {
        observable: 'Ext.util.Observable'
    },
    config: {
        containerPath: null,
        schemaName: null,
        queryName: null,
        sql: null,
        viewName: null,
        filterArray: null,
        sortArray: null,
        containerFilter: null,
        dataRegionName: 'query',
        loadOnCreate: true
    },

    statics: {
        /**
         * Uses an Ext reader to type-convert the results of LABKEY.Query.selectRows into an array of Ext models
         * @param results {LABKEY.Query.ExtendedSelectRowsResults} The results of a selectRows() call.  NOTE: you must use ExtendedSelectRowsResults,
         * which is obtained by using 'requiredVersion: 9.1' in your selectRows() config
         * @return {*}
         */
        getRecordsFromSelectRows: function(results){
            //use reader for type-conversion
            var reader = Ext4.create('LABKEY.ext4.ExtendedJsonReader', {});
            var data =  reader.readRecords(results);
            return data.records;
        },

        /**
         * Similar to getRecordsFromSelectRows(), except this returns an array of maps, rather than Ext records
         * @param results {LABKEY.Query.ExtendedSelectRowsResults}
         * @return {Array}
         */
        getRowMapsFromSelectRows: function(results){
            var records = LDK.QueryHelper.getRecordsFromSelectRows(results);
            var maps = [];
            for (var i=0;i<records.length;i++){
                maps.push(records[i].data);
            }
            return maps;
        },

        getQueryConfigFromDataRegion: function(dataRegion){
            return {
                schemaName: dataRegion.schemaName,
                queryName: dataRegion.queryName,
                viewName: dataRegion.viewName,
                filterArray: dataRegion.getUserFilterArray(),
                sort: dataRegion.getUserSort(),
                containerFilter: dataRegion.getUserContainerFilter()
            }
        }
    },

    _store: null,
    _editable: null,
    _metadata: null,
    _columnMap: null,
    _views: null,


    constructor: function(config){
        if (config.sort){
            if (Ext4.isString(config.sort)){
                config.sortArray = config.sort.split(',');
            }
            else {
                config.sortArray = config.sort;
            }
            config.sort = null;
        }
        this.initConfig(config);

        if(this.loadOnCreate){
            this.loadQueryInfo();
        }

        this.addEvents('filterChange', 'sortChange');
        this.callParent();
    },

    loadQueryInfo: function(){

    },

    createStore: function(){
        if(this._store)
            return this._store;

        return this._store = Ext4.create('LABKEY.ext4.data.Store', {
            containerPath: this.containerPath,
            schemaName: this.schemaName,
            queryName: this.queryName,
            viewName: this.viewName,
            filterArray: this.filterArray,
            sort: this.sortArray ? this.sortArray.join(',') : null
        });
    },

    getStore: function(){
        return this._store ? this._store : this.createStore();
    },

    getUrlParams: function(){

    },

    getQueryDetails: function(){

    },

    getColumns: function(){
        return this.columns
    },

    getFilterArray: function(){
        return this.filterArray;
    },

    appendFilter: function(filter){
        if (!this.filterArray){
            this.filterArray = [filter];
        }
        else {
            this.filterArray = LABKEY.Filter.merge(this.filterArray, filter.getColumnName(), [filter]);
        }
    },

    removeFilter: function(filter){

    },

    getViews: function(){

    },

    selectRows: function(config){

    },

    insertRows: function(config){

    },

    deleteRows: function(config){

    },

    saveRows: function(config){

    }
});
