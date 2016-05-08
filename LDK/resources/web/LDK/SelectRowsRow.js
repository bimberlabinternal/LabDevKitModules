/**
 * Experimental.  An attempt to see if we can make a more object oriented interface for the results of a selectRows call.
 * Features include:
 * - case insensitivity of field names
 * - coalescing of displayValue / value
 * - some formatting
 *
 * @param row
 * @return {Object}
 * @constructor
 */
LDK.SelectRowsRow = function(row){
    var propMap = {};
    var rawRow = row;
    for (var prop in row){
        propMap[prop.toLowerCase()] = prop;
    }

    function getRawValue(name){
        var normalized = propMap[name.toLowerCase()];
        if (!Ext4.isDefined(normalized))
            return null;

        return rawRow[normalized];
    }

    return {
        getValue: function(name){
            var val = getRawValue(name);
            if (!val)
                return null;

            return val.value;
        },

        getDisplayValue: function(name){
            var val = getRawValue(name);
            if (!val)
                return null;

            return Ext4.isEmpty(val.displayValue) ? (Ext4.isEmpty(val.value) ? null : val.value) : val.displayValue;
        },

        normalizeFieldName: function(name){
            return propMap[name.toLowerCase()];
        },

        getDateValue: function(name){
            return LDK.ConvertUtils.parseDate(this.getValue(name));
        },

        getURL: function(name){
            var val = getRawValue(name);
            if (!val)
                return null;

            return val.url;
        },

        getFormattedDateValue: function(name, formatString){
            var date = this.getDateValue(name);
            return date ? Ext4.Date.format(date, formatString) : date;
        }
    }
};