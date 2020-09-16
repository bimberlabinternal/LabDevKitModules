Ext4.ns('LDK.StoreUtils');

/**
 * A static helper with methods designed to assist with using an Ext4 store.  This allows us to augment the store without depending on
 * custom methods, meaning we are not tied exclusively to LABKEY.ext4.data.Store
 */
LDK.StoreUtils = new function(){


    return {
        /**
         *
         * @param config
         * @param config.store
         * @param config.text
         * @param config.delimiter Defaults to tab
         */
        getModelsFromText: function(config){
            config.delimiter = config.delimiter || '\t';

            var text = Ext4.String.trim(config.text);
            var data = LDK.Utils.CSVToArray(text, config.delimiter);
            var header = data.shift();
            var cannonicalNames = {};
            var metadataMap = {};
            var models = [];
            Ext4.each(header, function(fieldName){
                cannonicalNames[fieldName] = LDK.StoreUtils.getCanonicalFieldName(config.store, fieldName);
                metadataMap[fieldName] = LABKEY.ext4.Util.findFieldMetadata(config.store, cannonicalNames[fieldName]);
            }, this);

            for (var i=0;i<data.length;i++){
                var row = data[i];
                var recordData = {};
                for (var j=0;j<row.length;j++){
                    var fieldName = header[j];
                    var val = row[j];
                    recordData[cannonicalNames[fieldName]] = val;
                }

                var model = LDK.StoreUtils.createModelInstance(config.store, recordData, true);
                models.push(model);
            }

            return models;
        },


        addTSVToStore: function(config){
            var models = LDK.Utils.getModelsFromText(config);
            config.store.add(models);
        },

        /**
         * A helper that will generate an excel template, based on an array of field config objects.  It will only include
         * columns that are not hidden, and where shownInInsertView !== false.  An array of additional field names to skip
         * can also be provided.
         * @param config
         * @param config.fields
         * @param config.fileName
         * @param config.labelProperty
         * @param config.skippedColumns
         * @param config.includeDescription
         */
        createExcelTemplate: function(config){
            Ext4.applyIf(config, {
                labelProperty: 'caption'
            });

            var header = [];
            Ext4.each(config.fields, function(field){
                if (!field.isHidden && field.shownInInsertView !== false){
                    if (config.skippedColumns && Ext4.Array.contains(config.skippedColumns, field.name))
                        return;

                    //Sending label property and comment in case of including description
                    if (config.includeDescription){
                        header.push({
                            value:       field[config.labelProperty],
                            cellComment: field['description']
                        });
                    }
                    else{
                        header.push(field[config.labelProperty]);
                    }
                }
            }, this);

            LABKEY.Utils.convertToExcel({
                fileName : config.fileName,
                sheets : [{
                    name: 'data',
                    data: [header]
                }]
            });
        },

        getExcelTemplateColumns: function(store){
            var columns = [];
            store.getFields().each(function(field){
                if (!field.isHidden && field.shownInInsertView !== false)
                    columns.push(field.name);
            }, this);
            return columns;
        },

        /**
         * A helper that will create a new model instance, setting field defaults based on query metadata
         * @param store
         * @param setDefaults
         */
        createModelInstance: function(store, data, setDefaults){
            data = data || {};
            var model = store.createModel({});

            var fields = store.model.getFields();
            Ext4.each(fields, function(field){
                var val;
                if (!Ext4.isEmpty(data[field.name])){
                    val = data[field.name];
                    var type = Ext4.data.Types[field.extType];
                    if (type && type.convert){
                        if (field.extType == LABKEY.ext4.Util.EXT_TYPE_MAP.date)
                            val = LDK.ConvertUtils.parseDate(val);
                        else
                            val = type.convert(val);
                    }

                    model.set(field.name, val);
                    return;
                }

                if (setDefaults){
                    if (field.getInitialValue){
                        if (!Ext4.isFunction(field.getInitialValue)){
                            field.getInitialValue = eval('(' + field.getInitialValue + ')');
                        }
                        val = field.getInitialValue(null, model, field);
                        model.set(field.name, val);
                    }
                    else if (field.defaultValue)
                        model.set(field.name, field.defaultValue);
                }
            }, this);

            return model;
        },

        /**
         * Returns the case-normalized field name for a string.  This is useful because javascript is case-sensitive and incorrect casing can result in
         * unexpected behavior.  This method will also resolve the fieldName from the supplied string by matching against the following properties, in this
         * order of precedence: name, fieldKeyPath, label, caption.
         * @param {Ext.data.Store} store The store to query
         * @param {String} fieldName The string to test
         * @returns {String} The normalized field name or null if not found
         */
        getCanonicalFieldName: function(store, fieldName){
            var fields = store.getFields();
            if(fields.get(fieldName)){
                return fieldName;
            }

            var name;
            var properties = ['name', 'fieldKeyPath', 'label', 'caption'];
            Ext4.each(properties, function(prop){
                fields.each(function(field){
                    if(field[prop] && field[prop].toLowerCase() == fieldName.toLowerCase()){
                        name = field.name;
                        return false;
                    }
                });

                if(name)
                    return false;  //abort the loop
            }, this);

            return name;
        },

        /**
         * Returns a map of the fields in this store, keyed against the provided property
         * @param store
         * @param property The field property to be used as the key in the map.  Defaults to 'name'
         */
        getFieldMap: function(store, property){
            property = property || 'name';

            var map = {};
            store.getFields().each(function(field){
                map[field[property]] = field;
            }, this);

            return map;
        },

        /**
         * Parses a string into a date, normalizing for the current timezone.  Date.parse()
         * will make different timezone assumptions, depending on date format.  For example,
         * 2010-02-04 is assumed to be GMT, while 2/4/2010 is assume to match the local machine.
         * This method tries to infer the date format, and if an ISO date is provided, it will convert the
         * date object to the current timezone.
         * @param val
         */
        normalizeDateString: function(val){
            if (!val || Ext4.isDate(val)){
                return val;
            }
            else if (Ext4.isNumber(val)){
                return new Date(val);
            }

            else if (Ext4.isString(val)) {
                //try to guess format:
                var date;
                //ISO dates are assumed to be GMT, so we convert to local time, letting Ext normalize timezone
                if (Ext4.Date.parse(val, Date.patterns.ISO8601Long)){
                    date = Ext4.Date.parse(val, Date.patterns.ISO8601Long);
                }
                else if (Ext4.Date.parse(val, Date.patterns.ISO8601Short)){
                    date = Ext4.Date.parse(val, Date.patterns.ISO8601Short);
                }
                else if (val.indexOf('Z') != -1)
                {
                    var parsed = Date.parse(val);
                    if (parsed)
                        date = new Date(parsed);
                }
                else {
                    //with non ISO dates, browsers seem to accept tacking the timezone to the end
                    var parsed = Date.parse(val + ' ' + Ext4.Date.getTimezone(new Date()));
                    if (parsed)
                        date = new Date(parsed);
                }

                if (date){
                    var mills = Date.parse(Ext4.Date.format(date, 'm/d/Y H:i'));
                    if (!mills == date.getTime()){
                        console.error('Date doesn\'t match: ' + val + '/' + date.toString());
                        return null;
                    }
                    else {
                        return date;
                    }
                }
            }
            return val;
        },

        sortStoreByFieldNames: function(store, fieldNames){
            var fields = [];
            Ext4.each(fieldNames, function(fn){
                var meta = LABKEY.ext4.Util.findFieldMetadata(store, fn);
                if (meta)
                    fields.push(meta);
            }, this);

            var sort = LDK.StoreUtils.getStoreSortFn(fields);
            store.data.sortBy(sort);
            store.fireEvent('datachanged', store);
        },

        /**
         * A sorter function that can be used to sort an Ext store based on one or more fields.  The primary advantage is that this sorter uses the column
         * metadata to sort on the displayValue, instead of rawValue for lookup columns, which is usually what the user expects.
         * @param {array} fieldList An ordered array of field metadata objects.
         * @returns {function} The sorter function that can be passed to the sort() method of an Ext.data.Store.
         */
        getStoreSortFn: function(fields){
            var fieldList = [];
            Ext4.each(fields, function(meta){
                var obj = {term: meta.dataIndex || meta.name};
                if(meta.lookup){
                    Ext4.apply(obj, {
                        storeId: LABKEY.ext4.Util.getLookupStoreId(meta),
                        displayField: meta.lookup.displayColumn,
                        valueField: meta.lookup.keyColumn
                    });

                    //pre-create store if it does not exist
                    if (!Ext4.StoreMgr.get(obj.storeId)){
                        LABKEY.ext4.Util.getLookupStore(meta);
                    }
                }
                fieldList.push(obj);
            }, this);

            return function(a, b){
                var retVal = 0;
                Ext4.each(fieldList, function(item){
                    var val1 = '';
                    var val2 = '';
                    if(!item.storeId){
                        val1 = a.get(item.term) || '';
                        val2 = b.get(item.term) || '';
                    }
                    else {
                        var store = Ext4.StoreMgr.get(item.storeId);
                        var rec1;
                        var rec2;
                        rec1 = store.findExact(item.valueField, a.get(item.term));
                        if(rec1 != -1){
                            rec1 = store.getAt(rec1);
                            val1 = rec1.get(item.displayField) || '';
                        }
                        rec2 = store.findExact(item.valueField, b.get(item.term));
                        if(rec2 != -1){
                            rec2 = store.getAt(rec2);
                            val2 = rec2.get(item.displayField) || '';
                        }
                    }

                    if(val1 < val2){
                        retVal = -1;
                        return false;
                    }
                    else if (val1 > val2){
                        retVal = 1;
                        return false;
                    }
                    else {
                        retVal = 0;
                    }
                }, this);
                return retVal;
            }
        },

        getColumnConfigForField: function(field, config){
            config = config || {};
            var col = {};

            col.dataIndex = field.dataIndex || field.name;
            col.name = field.dataIndex || field.name;
            col.header = field.header || field.caption || field.label || field.name;
            col.customized = true;

            col.hidden = field.hidden;
            col.format = field.extFormat;

            //this.updatable can override col.editable
            col.editable = field.userEditable!==false;

            if(col.editable && !col.editor)
                col.editor = LABKEY.ext4.Util.getGridEditorConfig(field);

            col.renderer = LABKEY.ext4.Util.getDefaultRenderer(col, field);

            //HTML-encode the column header
            col.text = Ext4.util.Format.htmlEncode(col.header);

            if(field.ignoreColWidths)
                delete col.width;

            //allow override of defaults
            if(field.columnConfig)
                Ext4.Object.merge(col, field.columnConfig);

            if(config)
                Ext4.Object.merge(col, config);

            return col;
        },

        /**
         * Iterates the records in a store and returns true if any are dirty
         * @param store
         */
        isDirty: function(store){
            var recs = store.getRange();
            for (var i=0;i<recs.length;i++){
                if (recs[i].dirty)
                    return true;
            }
            return false;
        },

        getMissingRequiredFields: function(store, fields){
            var recs = store.getRange();
            var missing = [];

            if (!fields){
                fields = [];
                Ext4.each(store.model.getFields(), function(field){
                    if (field.nullable === false || field.allowBlank === false)
                        fields.push(field.dataIndex || field.name);
                }, this);
            }

            for (var i=0;i<recs.length;i++){
                for (var j=0;j<fields.length;j++){
                    if (Ext4.isEmpty(recs[i].get(fields[j]))) {
                        missing.push(fields[j]);
                    }
                }
            }
            missing = Ext4.unique(missing);
            return missing;
        },

        /**
         * Provides a way to remove a batch of records from a store, without firing a separate remove
         * event for each.  If this is used, listeners should listen for the bulkremove event instead
         * @param store
         * @param records
         */
        bulkRemove: function(store, records){
            var indexes = [];
            for (var i=0;i<records.length;i++){
                indexes.push(store.indexOf(records[i]));
            }

            store.suspendEvents();
            store.remove(records);
            store.resumeEvents();
            store.fireEvent('bulkremove', store, records, indexes);
            store.fireEvent('datachanged', store);


        },

        validateLookupValue: function(store, fieldName, combo){
            //first find the target store
            var allowable = [];
            if (combo && combo.store){
                allowable = allowable.concat(combo.store.collect(combo.valueField, false, true));
                if (allowable.length){
                    var msg;
                    store.each(function(rec){
                        var val = rec.get(fieldName);
                        if (!Ext4.isEmpty(val)){
                            if (allowable.indexOf(val) == -1){
                                msg = 'Unknown value for field ' + fieldName + ': ' + val;
                                return false;
                            }
                        }
                    }, this);

                    return msg;
                }
            }
        }
    }
}

