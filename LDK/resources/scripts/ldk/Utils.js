/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var console = require("console");
var LABKEY = require("labkey");

var LDK = {};
exports.LDK = LDK;

LDK.Server = {};
LDK.Server.Utils = new function(){

    return {
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
         * <li>floatvalue1: A numeric value.</li>
         * <li>floatvalue2: A numeric value.</li>
         * <li>floatvalue3: A numeric value.</li>
         * <li>stringvalue1: A string value.</li>
         * <li>stringvalue2: A string value.</li>
         * <li>stringvalue3: A string value.</li>
         */
        logMetric: function(config){
            if(!config || !config.metricName){
                alert('ERROR: No metric name provided');
                return;
            }

            LABKEY.ExtAdapter.apply(config, {
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
        }
    }
}

