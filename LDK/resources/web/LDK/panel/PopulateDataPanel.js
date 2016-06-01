Ext4.define('LDK.panel.PopulateDataPanel', {
    extend: 'Ext.panel.Panel',

    //override these
    moduleName: '',

    //per table, maintain a TSV in /resources/data of this module with the actual data.
    //these should not hold private data.  it is intended for simple lookup values only.
    //the file should be named schemaName-queryName.tsv
    tables: [],

    initComponent: function(){
        if (!this.tables){
            Ext4.Msg.alert('Error', 'No tables provided');
        }

        this.tables.sort(function(a, b) {
            return a.label.toLowerCase() < b.label.toLowerCase() ? -1 : 1;
        });

        // Keep lookup sets as the first item
        this.tables.splice(0, 0, {
            label: 'Lookup Sets',
            doSkip: true,
            populateFn: 'populateLookupSets',
            schemaName: 'ldk',
            queryName: 'lookup_sets'
        });

        Ext4.apply(this, {
            pendingInserts: 0,
            pendingDeletes: 0,
            defauts: {
                border: false
            },
            border: false,
            items: this.getItems()
        });

        this.callParent(arguments);
    },

    getItems: function(){
        var tableItems = [];
        var items = [{
            layout: 'hbox',
            border: false,
            items: [{
                border: false,
                layout: {
                    type: 'table',
                    columns: 2
                },
                defaults: {
                    style: 'margin: 2px;'
                },
                items: tableItems
            },{
                border: false,
                itemId: 'msgItem',
                xtype: 'box',
                width: "400px",
                style: {
                    overflowY: "scroll"
                },
                html: '<div id="msgbox"></div>'
            }]
        }];

        Ext4.each(this.tables, function(table){
            table.schemaName = table.schemaName || 'lookups';
            table.populateFn = table.populateFn || 'populateFromFile';
            table.pk = table.pk || 'rowid';

            tableItems.push({
                xtype: 'button',
                text: 'Populate ' + table.label,
                scope: this,
                handler: function(){
                    document.getElementById('msgbox').innerHTML = '<div>Populating ' + table.queryName + '...</div>';
                    if (table.populateFn == 'populateFromFile') {
                        this.populateFromFile(table.schemaName, table.queryName);
                    } else {
                        this[table.populateFn].call(this);
                    }
                }
            });

            tableItems.push({
                xtype: 'button',
                text: 'Delete Data From ' + table.label,
                scope: this,
                handler: function(){
                    document.getElementById('msgbox').innerHTML = '<div>Deleting ' + table.label + '...</div>';
                    this.deleteHandler(table);
                }
            });
        }, this);

        tableItems.push({
            xtype: 'button',
            text: 'Populate All',
            scope: this,
            handler: function(){
                document.getElementById('msgbox').innerHTML = '';
                Ext4.each(this.tables, function(table){
                    if (!table.doSkip) {
                        document.getElementById('msgbox').innerHTML += '<div>Populating ' + table.queryName + '...</div>';
                        if (table.populateFn == 'populateFromFile') {
                            this.populateFromFile(table.schemaName, table.queryName);
                        } else {
                            this[table.populateFn]();
                        }
                    } else {
                        document.getElementById('msgbox').innerHTML += '<div>Skipping ' + table.label + '</div>';
                        console.log('skipping: ' + table.label)
                    }
                }, this);
            }
        });
        tableItems.push({
            xtype: 'button',
            text: 'Delete All',
            scope: this,
            handler: function(){
                this.pendingDeletes = 0;
                document.getElementById('msgbox').innerHTML = '';
                Ext4.each(this.tables, function(table){
                    if (!table.doSkip) {
                        document.getElementById('msgbox').innerHTML += '<div>Deleting ' + table.label + '...</div>';
                        this.deleteHandler(table);
                    } else {
                        document.getElementById('msgbox').innerHTML += '<div>Skipping ' + table.label + '</div>';
                        console.log('skipping: ' + table.label);
                    }
                }, this);
            }
        });
        console.log(items);
        return items;
    },

    deleteHandler: function(table){
        if (table.deleteFn){
            table.deleteFn.call(this);
        }
        else {
            this.truncate(table.schemaName, table.queryName);
        }
    },

    truncate: function (schemaName, queryName) {
        this.pendingDeletes++;
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL("query", "truncateTable.api"),
            success: LABKEY.Utils.getCallbackWrapper(this.onDeleteSuccess, this),
            failure: LDK.Utils.getErrorCallback({
                callback: function (resp) {
                    document.getElementById('msgbox').innerHTML += '<div class="labkey-error">Error loading data: ' + resp.errorMsg + '</div>';
                },
                scope: this
            }),
            jsonData: {
                schemaName: schemaName,
                queryName: queryName
            },
            headers: {
                'Content-Type': 'application/json'
            }
        });
    },

    onDeleteSuccess: function(data){
        var count = data ? (data.affectedRows || data.deletedRows) : '?';
        console.log('success deleting ' + count + ' rows: ' + (data ? data.queryName : ' no query'));
        this.pendingDeletes--;
        if (this.pendingDeletes==0){
            document.getElementById('msgbox').innerHTML += '<div>Delete Complete</div>';
        }
    },

    populateLookupSets: function(){
        this.pendingInserts++;

        //records for reports:
        var config = {
            schemaName: 'ldk',
            queryName: 'lookup_sets',
            moduleResource: '/data/ldk-lookup_sets.tsv',
            success: this.onSuccess,
            failure: this.onError,
            scope: this
        };

        var origSuccess = config.success;
        config.success = function(results, xhr, c) {
            console.log('lookup set records inserted');

            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('admin', 'caches', '/'),
                method:  'post',
                params: {
                    clearCaches: 1
                },
                scope: this,
                success: function(){
                    console.log('cleared caches');
                    origSuccess.call(config.scope, results, xhr, c);
                },
                failure: function(){
                    console.error(arguments);
                }
            });
        };

        this.importFile(config);
    },

    populateFromFile: function (schemaName, queryName) {
        console.log("Populating " + schemaName + "." + queryName + "...");
        this.pendingInserts++;
        //records for task forms:
        var config = {
            schemaName: schemaName,
            queryName: queryName,
            moduleResource: '/data/' + schemaName + '-' + queryName + '.tsv',
            success: this.onSuccess,
            failure: this.onError,
            scope: this
        };

        this.importFile(config);
    },

    importFile: function(config) {
        var o = {
            schemaName: config.schemaName,
            queryName: config.queryName
        };
console.log(config.moduleResource)
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL("query", "import", config.containerPath, {
                module: this.moduleName,
                moduleResource: config.moduleResource
            }),
            method: 'POST',
            timeout: 100000,
            success: LABKEY.Utils.getCallbackWrapper(config.success, config.scope),
            failure: LABKEY.Utils.getCallbackWrapper(config.failure, config.scope, true),
            jsonData: o,
            headers: {
                'Content-Type': 'application/json'
            }
        });
    },

    onSuccess: function(result, xhr, config){
        if (result.exception || result.errors) {
            // NOTE: importFile uses query/import.view which returns statusCode=200 for errors
            this.onError.call(this, result, xhr, config);
        } else {
            this.pendingInserts--;

            var queryName = result.queryName || config.queryName || config.jsonData.queryName;
            console.log('Success ' + (result.rowCount !== undefined ? result.rowCount + ' rows: ' : ': ') + queryName);

            if (this.pendingInserts == 0) {
                document.getElementById('msgbox').innerHTML += '<div>Populate Complete</div>';
            }
        }
    },

    onError: function(result, xhr, config){
        this.pendingInserts--;

        var queryName = result.queryName || config.queryName || config.jsonData.queryName;
        console.log('Error Loading Data: '+ queryName);
        console.log(result);

        document.getElementById('msgbox').innerHTML += '<div class="labkey-error">ERROR: ' + queryName + ': ' + result.exception + '</div>';

        if (this.pendingInserts==0){
            document.getElementById('msgbox').innerHTML += '<div>Populate Complete</div>';
        }
    }
});
