Ext4.ns('LDK');

/**
 * Static helpers designed to help with type conversion in JS.
 */
LDK.ConvertUtils = new function(){
    var DATEFORMATS = LABKEY.Utils.getDateAltFormats().split('|');
    DATEFORMATS.push('Y/m/d H:i:s');
    DATEFORMATS.push('n-j-Y');
    DATEFORMATS.push('m-d-Y');
    DATEFORMATS.push('m/d/Y');
    DATEFORMATS.push('m/j/Y');
    DATEFORMATS.push('m/d/y');
    DATEFORMATS.push('m/j/y');

    //@private

    //adapted from Ext.field.Date.  will parse a date in the given format, returning null if it does not match
    function safeParseDate(value, format, useStrict){
        var result = null,
                parsedDate;

        if (value instanceof Date) {
            return value;
        }

        useStrict = Ext4.isDefined(useStrict) ? useStrict : false;

        if (Ext4.Date.formatContainsHourInfo(format)) {
            // if parse format contains hour information, no DST adjustment is necessary
            result = Ext4.Date.parse(value, format, useStrict);
        } else {
            // The core of the parsing problem comes from JS Date.parse() treating ISO 8601 short differently from other date formats:
            // https://www.w3.org/TR/NOTE-datetime
            // Example:
            // new Date('2024-01-01')
            // Sun Dec 31 2023 16:00:00 GMT-0800 (Pacific Standard Time)
            // new Date('1/1/2024')
            // Mon Jan 01 2024 00:00:00 GMT-0800 (Pacific Standard Time)

            // Therefore special case this format and append the browser's time zone:
            if (format === 'c' && value.length === 10) {
                format = 'Y-m-d';
            }

            parsedDate = Ext4.Date.parse(value, format, useStrict);
            if (parsedDate) {
                result = Ext4.Date.clearTime(parsedDate);
            }
        }
        return result;
    }



    //public

    /**
     * Will parse a date string, returning null if it cannot be parsed.  If provided, it will first attempt
     * to parse using the supplied format string.  If this does not produce a valid date, it will iterate all format
     * strings in LABKEY.Utils.getDateAltFormats(), trying each one until a valid date is parsed.
     * @param {String} value The value to be parsed
     * @param {String} [format] An option format string that will be used first.  If null or if this does not produce a valid date, the format
     * string from LABKEY.Utils.getDateAltFormats() will be attempted in order.
     */
    return {
        parseDate : function(value, format) {
            if(!value || Ext4.isDate(value)){
                return value;
            }

            var formats = [];
            if (format)
                formats.push(format);
            formats = formats.concat(DATEFORMATS);

            // See 'c' format: https://docs.sencha.com/extjs/4.2.1/#!/api/Ext.Date
            // And http://www.w3.org/TR/NOTE-datetime
            // The issue is that this treats any date beginning with YYYY-MM-DD as ISO8601,
            // and assumes GMT as the date/time. In general, we want the string '2024-01-01' to be treated as the browser's timezone.
            // Therefore push this to the lowest priority, which will allow other formats to preferentially parse date-only values
            if (format !== 'c' && formats.indexOf('c') > -1) {
                formats = Ext4.Array.remove(formats, 'c')
                formats.push('c')
            }

            var val;
            for (var i=0; i < formats.length; ++i) {
                val = safeParseDate(value, formats[i]);
                if (val) {
                    break;
                }
            }

            // two digit years tend to get parsed as 1900, rather than 2000s, so we make assumptions about dates more than 90 in the past
            if (val && (val.getFullYear() < new Date().getFullYear() - 90))
                val.setFullYear(val.getFullYear() + 100);

            return val;
        },
        
        parseDatesInSelectRowsResults: function(results){
            if (!results.rows || !results.metaData || !results.metaData.fields){
                return;
            }

            Ext4.Array.forEach(results.metaData.fields, function(field){
                if (field.jsonType === 'date'){
                    Ext4.Array.forEach(results.rows, function(row){
                        if (row[field.name]){
                            row[field.name] = LDK.ConvertUtils.parseDate(row[field.name]);
                        }
                    }, this);
                }
            }, this);
        }
    }
};