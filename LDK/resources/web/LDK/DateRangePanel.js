/**
 * A panel that will display a pair of datefields, either side by side or above one another.  These fields will automatically
 * set the min/max date on one another to ensure a valid date range.
 */
Ext4.define('LDK.ext.DateRangePanel', {
    extend: 'Ext.form.Panel',
    alias: 'widget.ldk-daterangepanel',
    dateFormat: LABKEY.extDefaultDateFormat, //YYYY-MMM-DD
    initComponent: function(){
        Ext4.apply(this, {
            border: false,
            layout: 'vbox',
            items: [{
                tag: 'div',
                border: false,
                html: 'From:'
            },
                this.getFieldConfig('start')
            ,{
                tag: 'div',
                border: false,
                html: 'To:'
            },
                this.getFieldConfig('end')
            ]
        });

        this.callParent(arguments);
    },

    getFieldConfig: function(prefix){
        var pairId = prefix == 'start' ? 'endDateField' : 'startDateField';
        return {
            xtype: 'datefield',
            itemId: prefix + 'DateField',
            format: this.dateFormat,
            width: 165,
            allowBlank:true,
            scope: this,
            validator: function(val){
                var date = this.parseDate(val);
                if (!date)
                    return;

                var pairField = this.up('panel').down('#' + pairId);
                if (this.itemId == 'endDateField'){
                    pairField.setMaxValue(date);
                }
                else if (this.itemId == 'startDateField'){
                    pairField.setMinValue(date);
                }

                //Always return true since we're only using this vtype to set the min/max allowed values
                return true;
            }
        }
    },

    getStartDate: function(){
        return this.down('#startDate').getValue();
    },

    getEndDate: function(){
        return this.down('#endDate').getValue();
    }
});