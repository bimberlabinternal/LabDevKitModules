Ext4.namespace('LDK.Utils');

LDK.Utils = new function(){
    return {
        /**
          * Experimental. Designed to be used to create error callbacks for AJAX calls.  Contains config options that
          * control how this error is logged and displayed.
          * @param config
          * <li>msgPrefix {String} A prefix that will be appended to the error message
          * <li>showAlertOnError {Boolean} If true, an alert window will appear on error.  Defaults to false
          * <li>hideMsg {Boolean} If true, any pre-existing Ext4.Msg alerts, such as a wait dialog, will be hidden on error.  Defaults to true.
          * <li>showExceptionClass {Boolean} If true, the exception class will be appended to the error message, if present
          * <li>callback {Function} An optional callback function that will be called on error
          * <li>scope {Object} The optional scope of the callback.  Defaults to 'this'
          * <li>logToServer {Boolean} If true, this error will be logged to the server.  Default to true.
          */
        getErrorCallback: function(config){
             config = config || {};
             return function(){
                 var responseObj, request, exception;
                 //NOTE: some failure callbacks append an exception object as the first argument, but Ext passes the response in this position
                 if(arguments.length == 3){
                    exception = arguments[0];
                    responseObj = arguments[1];
                 }
                 else {
                     exception = null;
                     responseObj = arguments[0];
                 }

                 if (config.hideMsg !== false && Ext4.Msg.isVisible())
                     Ext4.Msg.hide();

                 if (!responseObj || !responseObj.status || responseObj.status == 0){
                     console.log('ignoring error');
                     console.log(arguments);
                     return;  //the user probably navigated from the page.
                 }

                 var detailedError = LABKEY.Utils.getMsgFromError(responseObj, exception, {
                     showExceptionClass: true,
                     msgPrefix: config.msgPrefix
                 });
                 detailedError = '[' + detailedError + ']';

                 var errorMsg = LABKEY.Utils.getMsgFromError(responseObj, null, {
                     showExceptionClass: config.showExceptionClass,
                     msgPrefix: config.msgPrefix
                 });
                 responseObj.errorMsg = errorMsg;

                 var stackTrace = responseObj && responseObj.responseJSON ? responseObj.responseJSON.stackTrace : null;

                 if(config.logToMothership){
                     LDK.Utils.logToMothership({
                         msg: detailedError,
                         stackTrace: stackTrace,
                         file: null,
                         line: null
                     });
                 }

                 if(config.logToServer !== false){
                    LDK.Utils.logToServer({
                        message: detailedError,
                        level: 'ERROR',
                        includeContext: true
                    });
                 }

                 if(config.callback)
                     config.callback.call(config.scope || this, responseObj);

                 //dont use Ext.Msg.alert so tests will fail if we hit this
                 if(config.showAlertOnError !== false)
                     alert(errorMsg);

                 console.log(arguments);
             }
        },

        /**
         * A shortcut to log an error message to the server's error log.  It will automatically include context about the page.
         * @param msg
         */
        logError: function(msg){
            LDK.Utils.logToServer({
                message: msg,
                level: 'ERROR',
                includeContext: true
            });
        },

        /**
         * Experimental. A helper to log errors to mothership, LABKEY's central reporting mechanism.  This should not be used
         * unless you are familiar with mothership.
         * @param config An object with the information to be logged.  Can contain the following properties:
         * <li>msg
         * <li>stacktrace
         * <li>file
         * <li>line
         */
        logToMothership: function(config){
            if(Ext4.isArray(config.stackTrace)){
                config.stackTrace = config.stackTrace.join('\n');
            }
            var params = {
                username: LABKEY.user ? LABKEY.user.email : "Unknown",
                exceptionMessage: 'Client error: ' + config.msg,
                stackTrace: config.stackTrace || config.msg || 'No message provided',
                file: config.file,
                line: config.line,
                requestURL: config.file,
                referrerURL: document.URL,
                browser: navigator && navigator.userAgent || "Unknown",
                platform:  navigator && navigator.platform  || "Unknown"
            }

            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('admin', 'logClientException', null, params),
                method : 'GET',
                scope: this,
                failure: function(response){
                    console.log('unable to log to mothership');
                    console.log(response);
                }
            });
        },

        /**
         * A utility that allows client-side code to log messages to the server's log.  This can
         * be used to log any information, but was originally designed to allow client-side code
         * to log errors to a central location.  The user must have ReadPermission in the current container
         * to log this error.
         * @param config The config object, which supports the following options:
         * <li>message: The message to log</li>
         * <li>level: The error level, either ERROR, WARN, INFO or DEBUG.  Defaults to ERROR</li>
         * <li>includeContext: If true, the following will automatially be appended to the message string: URL of the current page, Browser, Platform.  Defaults to true
         */
        logToServer: function(config){
            if (!config || !config.message){
                alert('ERROR: Must provide a message to log');
            }
            if (LABKEY.Security.currentUser.isGuest){
                console.log('Guests cannot write to the server log');
                return;
            }

            if(config.includeContext !== false){
                config.message += '\n' + [
                    "User: " + LABKEY.Security.currentUser.email,
                    "ReferrerURL: " + document.URL,
                    ("Browser: " + (navigator && navigator.userAgent || "Unknown")),
                    ("Platform: " + (navigator && navigator.platform  || "Unknown"))
                ].join('\n');
            }

            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('admin', 'log'),
                params: {
                    message: config.message,
                    level: config.level
                },
                method : 'POST',
                scope: this,
                failure: function(response){
                    console.error('Unable to log message to server');
                    console.error(response);
                }
            });
        },

        /**
         * This is a barebones API designed to allow client-side code to log metrics to the server, such
         * as performance data on a particular application.  These records are inserted into the table
         * ldk.perf_metrics.  The advantage of this API opposed to insertRows() is that this API requires
         * only ReadPermission in order to insert the data.
         *
         * In addition to the information provided, this will log the current user, container and current URL
         *
         * @param config The config object.  It supports the following properties:
         * <li>category: An arbitrary category name of this metric.</li>
         * <li>metricName: The name of the metric.  required.</li>
         * <li>numericValue1: A numeric value.</li>
         * <li>numericValue2: A numeric value.</li>
         * <li>numericValue3: A numeric value.</li>
         * <li>stringValue1: A string value.</li>
         * <li>stringValue2: A string value.</li>
         * <li>stringValue3: A string value.</li>
         */
        logMetric: function(config){
            if(!config || !config.metricName){
                alert('ERROR: No metric name provided');
                return;
            }

            if (LABKEY.Security.currentUser.isGuest){
                console.log('Guests cannot write to the server log');
                return;
            }

            Ext4.apply(config, {
                referrerURL: document.URL,
                browser: navigator && navigator.userAgent || "Unknown",
                platform:  navigator && navigator.platform  || "Unknown"
            });

            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('ldk', 'logMetric'),
                params: config,
                method : 'POST',
                scope: this,
                failure: function(response){
                    console.log('unable to log metric');
                    console.log(response);
                }
            });
        },

        /**
         * Returns A LABKEY.QueryWebPart using a boiler plate config with a handful of default settings
         * @param config An additional config object to be applied when creating the QWP
         */
        getBasicQWP: function(config){
            config = config || {};
            var cfg = Ext4.apply({
                allowChooseQuery: false,
                showRecordSelectors: true,
                buttonBarPosition: 'top',
                frame: 'none',
                timeout: 0,
                failure: LDK.Utils.getErrorCallback({
                    logToServer: true
                })
            }, config);

            if(config.title && !Ext4.isDefined(config.frame))
                cfg.frame = 'portal';

            return (new LABKEY.QueryWebPart(cfg));
        },

            /**
         * Returns A LABKEY.QueryWebPart using a boiler plate config with the query button and all edit UI turned off
         * @param config An additional config object to be applied when creating the QWP
         */
        getReadOnlyQWPConfig: function(config){
            config = config || {};
            var cfg = Ext4.apply({
                allowChooseQuery: false,
                allowChooseView: true,
                showRecordSelectors: true,
                showDetailsColumn: true,
                showUpdateColumn: false,
                showInsertNewButton: false,
                showDeleteButton: false,
                buttonBarPosition: 'top',
                frame: 'none',
                timeout: 0,
                failure: LDK.Utils.getErrorCallback({
                    logToServer: true
                })
            }, config);

            if(config.title && !Ext4.isDefined(config.frame))
                cfg.frame = 'portal';

            return cfg
        },

        getReadOnlyQWP: function(config){
            var cfg = LDK.Utils.getReadOnlyQWPConfig(config);
            return (new LABKEY.QueryWebPart(cfg));
        },

        /**
         * A helper to parse tabular text into an array of arrays representing the rows.
         * Adapted from: http://stackoverflow.com/questions/1293147/javascript-code-to-parse-csv-data
         * @param {String} strData The string to parse
         * @param {String} [strDelimiter] The delimiter to use.  Defaults to comma.
         */
        CSVToArray: function(strData, strDelimiter){
            // Check to see if the delimiter is defined. If not,
            // then default to comma.
            strDelimiter = (strDelimiter || ",");

            // Create a regular expression to parse the CSV values.
            var objPattern = new RegExp(
                    (
                            // Delimiters.
                            "(\\" + strDelimiter + "|\\r?\\n|\\r|^)" +

                            // Quoted fields.
                            "(?:\"([^\"]*(?:\"\"[^\"]*)*)\"|" +

                            // Standard fields.
                            "([^\"\\" + strDelimiter + "\\r\\n]*))"
                    ),
                    "gi"
                    );


            // Create an array to hold our data. Give the array
            // a default empty first row.
            var arrData = [[]];

            // Create an array to hold our individual pattern
            // matching groups.
            var arrMatches = null;


            // Keep looping over the regular expression matches
            // until we can no longer find a match.
            while (arrMatches = objPattern.exec( strData )){

                    // Get the delimiter that was found.
                    var strMatchedDelimiter = arrMatches[ 1 ];

                    // Check to see if the given delimiter has a length
                    // (is not the start of string) and if it matches
                    // field delimiter. If id does not, then we know
                    // that this delimiter is a row delimiter.
                    if (
                            strMatchedDelimiter.length &&
                            (strMatchedDelimiter != strDelimiter)
                            ){

                            // Since we have reached a new row of data,
                            // add an empty row to our data array.
                            arrData.push( [] );

                    }


                    // Now that we have our delimiter out of the way,
                    // let's check to see which kind of value we
                    // captured (quoted or unquoted).
                    if (arrMatches[ 2 ]){

                            // We found a quoted value. When we capture
                            // this value, unescape any double quotes.
                            var strMatchedValue = arrMatches[ 2 ].replace(
                                    new RegExp( "\"\"", "g" ),
                                    "\""
                                    );

                    } else {

                            // We found a non-quoted value.
                            var strMatchedValue = arrMatches[ 3 ];

                    }


                    // Now that we have our value string, let's add
                    // it to the data array.
                    arrData[ arrData.length - 1 ].push( strMatchedValue );
            }

            // Return the parsed data.
            return( arrData );
        },

        /**
         * Returns the current URL, encoded, minus the origin, which is suitable to use as a srcURL param.
         */
        getSrcURL: function(){
            var re = new RegExp('^' + window.location.origin);
            return encodeURIComponent(window.location.href.replace(re, ''));
        },

        /**
         * Create immutable object.  Usage:
         * var sealedObj = new LABKEU.Utils.sealed(obj);
         *
         * @param obj The object to seal
         * @returns The sealed object
         */
        sealed: function (obj) {
            function copy(o){
                var n = {};
                for(p in o){
                    n[p] = o[p]
                }
                return n;
            }
            var priv = copy(obj);
            return function(p) {
                return typeof p == 'undefined' ? copy(priv) : priv[p];  // or maybe copy(priv[p])
            }
        },

        EXT_TYPE_MAP:  {
            'string': 'STRING',
            'int': 'INT',
            'float': 'FLOAT',
            'date': 'DATE',
            'boolean': 'BOOL'
        },

        /**
         * Returns the correct ExtJS datatype from a LabKey JSONType
         * @param type
         */
        getExtDataType: function(type){
            return LDK.Utils.EXT_TYPE_MAP[type];
        },

        decodeHttpResponseJson: function(response){
            var json = response.responseJSON;
            if (!json)
            {
                //ensure response is JSON before trying to decode
                if(response && response.getResponseHeader && response.getResponseHeader('Content-Type')
                        && response.getResponseHeader('Content-Type').indexOf('application/json') >= 0){
                    try {
                        json = Ext4.decode(response.responseText);
                    }
                    catch (error){
                        //we still want to proceed even if we cannot decode the JSON
                    }

                }

                response.responseJSON = json;
            }
            return response.responseJSON;
        },

        /**
         * Sorts the passed array by the supplied property, returnin the sorted array
         * @param {array} arr The array to sort
         * @param {string} propName The name of the property on which to sort
         * @param {boolean} [caseSensitive] Flag to force items to be sorted in a case sensitive manner.  Defaults to false
         */
        sortByProperty: function(arr, propName, caseSensitive){
            return arr.sort(function(a, b){
                var a1 = !Ext4.isString(a[propName]) ? a[propName] : (caseSensitive ? a[propName] : a[propName].toLowerCase());
                var b1 = !Ext4.isString(b[propName]) ? b[propName] : (caseSensitive ? b[propName] : b[propName].toLowerCase());

                return a1 > b1 ? 1 :
                        a1 < b1 ? -1 : 0;
            });
        },

        getNotificationDetails: function(config){
            return LABKEY.Ajax.request({
                url : LABKEY.ActionURL.buildURL('ldk', 'getNotifications', config.containerPath),
                method : 'POST',
                scope: config.scope,
                failure: LDK.Utils.getErrorCallback({
                    callback: config.failure,
                    scope: config.scope
                }),
                success: LABKEY.Utils.getCallbackWrapper(LABKEY.Utils.getOnSuccess(config), config.scope)
            });
        },

        isSharedProject: function(){
            return LABKEY.Security.currentContainer.path == ('/' + LABKEY.Security.getSharedContainer())
        },

        textToArray: function(text){
            if (Ext4.isEmpty(text))
                return [];

            text = Ext4.String.trim(text);
            text = text.replace(/[\s,;]+/g, ';');
            text = text.replace(/(^;|;$)/g, '');
            text = text.toLowerCase();

            if (text){
                text = text.split(';');
            }

            return text;
        },

        // returns a naturalized string often used for sorting.
        // adapted from: http://my.opera.com/GreyWyvern/blog/show.dml/1671288
        naturalize: function(t) {
            var tz = [], x = 0, y = -1, n, i, j;

            while (i = (j = t.charAt(x++)).charCodeAt(0)) {
                var m = (i == 46 || (i >=48 && i <= 57));
                if (m !== n) {
                    tz[++y] = "";
                    n = m;
                }
                tz[y] += j;
            }

            for (var k=0;k<tz.length;k++){
                if (Ext4.isNumeric(tz[k])){
                    tz[k] = Ext4.String.leftPad(tz[k], 26, '0');
                }
            }

            return tz.join('');
        },

        editUIButtonHandler: function(schemaName, queryName, dataRegionName, paramMap, copyFilters){
            var params = {
                schemaName: schemaName,
                'query.queryName': queryName
            };

            if (copyFilters !== false && dataRegionName){
                var array = LABKEY.DataRegions[dataRegionName].getUserFilterArray();
                if (array && array.length){
                    for (var i=0;i<array.length;i++){
                        var filter = array[i];
                        params[filter.getURLParameterName()] = filter.getURLParameterValue();
                    }
                }

                //append non-removeable filters
                if (LABKEY.DataRegions[dataRegionName].qwp && LABKEY.DataRegions[dataRegionName].qwp.filters){
                    var array = LABKEY.DataRegions[dataRegionName].qwp.filters;
                    for (var i=0;i<array.length;i++){
                        var filter = array[i];
                        params[filter.getURLParameterName()] = filter.getURLParameterValue();
                    }
                }
            }

            if (paramMap != null){
                for (var param in paramMap){
                    if (LABKEY.ActionURL.getParameter(param)){
                        params[paramMap[param]] = LABKEY.ActionURL.getParameter(param);
                    }
                }
            }

            window.location = LABKEY.ActionURL.buildURL('ldk', 'updateQuery', null, params);
        },

        splitIds: function(subjectArray, unsorted){
            if (!subjectArray){
                return [];
            }

            subjectArray = Ext4.String.trim(subjectArray);
            subjectArray = subjectArray.replace(/[\s,;]+/g, ';');
            subjectArray = subjectArray.replace(/(^;|;$)/g, '');
            subjectArray = subjectArray.toLowerCase();

            if (subjectArray){
                subjectArray = subjectArray.split(';');
            }
            else {
                subjectArray = [];
            }

            if (subjectArray.length > 0) {
                subjectArray = Ext4.unique(subjectArray);
                if (!unsorted) {
                    subjectArray.sort();
                }
            }

            return subjectArray;
        }
    }
}
