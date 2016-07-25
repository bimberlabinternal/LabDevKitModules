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

        Ext4.apply(this, {
            pendingInserts: 0,
            pendingDeletes: 0,
            defauts: {
                border: false
            },
            border: false,
            items: [{
                html: 'This page is designed to provide simplified version control and deployment of lookup tables and other relatively invariant data.  ' +
                'The overall idea is that checked into your module you have TSVs (one per table) with the raw data.  These data can only be simple pick ' +
                'lists with columns for: value, display value (optional), and sort order (optional).  <br><br>' +

                '<b>Step 1:</b> When the developer created this page, he/she provided a list of tables for the server to create in the lookups schema.  ' +
                'The first time you use this page, you will need to tell the server to create these lookup tables.  ' +
                'Do this using the "Populate Lookup Sets" button (or "Delete Lookup Sets" to clear them).  This only needs to happen one time, unless you have added/removed tables.  If your admin has changed these tables, you should delete/populate this table again to refresh the set of tables.  ',
                style: 'padding-bottom: 20px;',
                border: false
            },this.getLookupSetButtons(),{
                html: '<b>Step 2:</b> Once the lookup tables exist, you can populate the data in those tables.  To do this, simply, click the Populate button for a given table, or use the "Populate All" button to import into all tables.  ' +
                'Note: the populate button will do a simple bulk insert to the table, meaning that if you have existing data you might get errors about primary key conflicts.  Use the Delete button first to remove all existing records and avoid this.  ' +
				'If you see error messages about a given table not existing, either retry Step 1 (which should create the expected tables), and/or use the schema browser to inspect the lookups schema and verify which tables are present.',
                style: 'padding-bottom: 20px;',
                border: false
            }].concat(this.getTableButtons())
        });

        this.callParent(arguments);
    },

	getLookupSetButtons: function(){
		return {
			border: false,
			style: 'padding-bottom: 20px;',
			layout: {
				type: 'table',
				columns: 2
			},
			defaults: {
				style: 'margin: 2px;'
			},
			items: [{
				xtype: 'button',
				text: 'Populate Lookup Sets',
				handler: this.populateLookupSets,
				scope: this
			},{
				xtype: 'button',
				text: 'Delete Data From Lookup Sets',
				handler: this.deleteLookupSets,
				scope: this
			}]
		}
	},

    getTableButtons: function(){
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

    truncate: function (schemaName, queryName, success) {
        this.pendingDeletes++;
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL("query", "truncateTable.api"),
            success: LABKEY.Utils.getCallbackWrapper(success || this.onDeleteSuccess, this),
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

	deleteLookupSets: function(){
		this.truncate('ldk', 'lookup_sets', this.onDeleteLookupSetsSuccess);
	},

	onDeleteLookupSetsSuccess: function(data){
		console.log('lookup set records deleted');

		LABKEY.Ajax.request({
			url: LABKEY.ActionURL.buildURL('admin', 'caches', '/'),
			method:  'post',
			params: {
				clearCaches: 1
			},
			scope: this,
			success: function(){
				console.log('cleared caches');
				this.onDeleteSuccess(data);
			},
			failure: function(){
				console.error(arguments);
			}
		});
	},

    populateLookupSets: function(){
        this.pendingInserts++;

        var rows = [];
        Ext4.each(this.tables, function(t){
            if (!t.queryName){
                console.warn('no queryName, skipping');
                console.warn(t);
                return;
            }

            rows.push({
                setname: t.queryName,
                label: t.label || t.queryName,
                description: t.description,
                keyField: t.keyField || 'value',
                titleColumn: t.titleColumn || 'value'
            });
        }, this);

        var config = {
            schemaName: 'ldk',
            queryName: 'lookup_sets',
            success: this.onSuccess,
            failure: this.onError,
            scope: this,
            rows: rows
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

        LABKEY.Query.insertRows(config);
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
