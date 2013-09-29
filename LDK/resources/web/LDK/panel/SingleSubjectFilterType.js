Ext4.define('LDK.panel.SingleSubjectFilterType', {
    extend: 'LDK.panel.AbstractFilterType',
    alias: 'widget.ldk-singlesubjectfiltertype',

    statics: {
        filterName: 'singleSubject',
        label: 'Single Subject'
    },

    initComponent: function(){
        this.items = this.getItems();

        this.callParent();
    },

    getItems: function(){
        var ctx = this.filterContext;
        var toAdd = [];

        toAdd.push({
            width: 200,
            html: 'Enter Subject Id:',
            style: 'margin-bottom:10px'
        });

        toAdd.push({
            xtype: 'panel',
            items: [{
                xtype: 'textfield',
                width: 165,
                itemId: 'subjArea',
                value: LABKEY.ExtAdapter.isArray(ctx.subjects) ? ctx.subjects.join(';') : ctx.subjects,
                listeners: {
                    scope: this,
                    render: function(field){
                        field.keyListener = this.tabbedReportPanel.createKeyListener(field.getEl());
                    }
                }
            }]
        });

        return toAdd;
    },

    getFilters: function(){
        return {
            subjects: this.getSubjects()
        }
    },

    getFilterArray: function(tab, subject){
        var filterArray = {
            removable: [],
            nonRemovable: []
        };

        var report = tab.report;
        var subjectFieldName = report.subjectFieldName;
        if (!subjectFieldName){
            //Ext4.Msg.alert('Error', 'This report does no provide the name of the field holding the subjectId');
            return;
        }

        var filters = this.getFilters();
        if (filters.subjects && filters.subjects.length){
            if (filters.subjects.length == 1)
                filterArray.nonRemovable.push(LABKEY.Filter.create(subjectFieldName, filters.subjects[0], LABKEY.Filter.Types.EQUAL));
            else
                filterArray.nonRemovable.push(LABKEY.Filter.create(subjectFieldName, filters.subjects.join(';'), LABKEY.Filter.Types.EQUALS_ONE_OF));
        }

        return filterArray;
    },

    checkValid: function(){
        var val = this.down('#subjArea').getValue();
        val = Ext4.String.trim(val);
        if(!val){
            Ext4.Msg.alert('Error', 'Must enter at least one subject ID');
            return false;
        };

        return true;
    },

    validateReport: function(report){
        if (!report.subjectFieldName)
            return 'This report cannot be used with the selected filter type, because the report does not contain a subject Id field';

        return null;
    },

    getTitle: function(){
        var subjects = this.getSubjects();
        if (subjects && subjects.length){
            if (subjects.length <= 6)
                return subjects.join(', ');

            return subjects.slice(0,5).join(', ') + '...';
        }

        return '';
    },

    getSubjects: function(){
        var subjectArray = this.down('#subjArea').getValue();
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

        if (subjectArray.length > 0){
            subjectArray = Ext4.unique(subjectArray);
            subjectArray.sort();
        }

        return subjectArray;
    }
});