Ext4.define('LDK.panel.MultiSubjectFilterType', {
    extend: 'LDK.panel.SingleSubjectFilterType',
    alias: 'widget.ldk-multisubjectfiltertype',

    statics: {
        filterName: 'multiSubject',
        label: 'Multiple Subjects'
    },

    initComponent: function(){
        this.items = this.getItems();

        this.callParent();

        //force subject list to get processed and append icons to left-hand panel on load
        this.getSubjects(this.tabbedReportPanel.getSubjects());
    },

    prepareRemove: function(){
        this.tabbedReportPanel.setSubjGrid();
    },

    getItems: function(){
        var ctx = this.filterContext || {};

        var toAdd = [];

        toAdd.push({
            width: 200,
            html: 'Enter Subject Id(s):<br><i>(Separated by commas, semicolons, space or line breaks)</i>'
        });

        toAdd.push({
            xtype: 'panel',
            layout: 'hbox',
            items: [{
                xtype: 'panel',
                width: 180,
                border: false,
                items: [{
                    xtype: 'textarea',
                    width: 180,
                    height: 100,
                    itemId: 'subjArea',
                    value: LABKEY.ExtAdapter.isArray(ctx.subjects) ? ctx.subjects.join(';') : ctx.subjects
                }]
            },{
                xtype: 'panel',
                layout: 'vbox',
                border: false,
                defaults: {
                    xtype: 'button',
                    width: 90,
                    buttonAlign: 'center',
                    bodyStyle:'align: center',
                    style: 'margin: 5px;'
                },
                items: [{
                    text: 'Append -->',
                    handler: function(btn){
                        var panel = btn.up('ldk-multisubjectfiltertype');
                        panel.getSubjects(panel.tabbedReportPanel.getSubjects());
                    }
                },{
                    text: 'Replace -->',
                    handler: function(btn){
                        var panel = btn.up('ldk-multisubjectfiltertype');
                        panel.getSubjects();
                    }
                },{
                    text: 'Clear',
                    handler: function(btn){
                        var panel = btn.up('ldk-multisubjectfiltertype');
                        panel.tabbedReportPanel.setSubjGrid();
                        panel.down('#subjArea').setValue();
                        panel.getSubjects();
                    }
                }]
            }]
        });

        return toAdd;
    },

    getSubjects: function(existing){
        //we clean up, combine, then split the subjectBox and subject inputs
        var subjectArray = this.down('#subjArea').getValue();
        subjectArray = subjectArray.replace(/[\s,;]+/g, ';');
        subjectArray = subjectArray.replace(/(^;|;$)/g, '');
        subjectArray = subjectArray.toLowerCase();

        if(subjectArray)
            subjectArray = subjectArray.split(';');
        else
            subjectArray = new Array();

        if (existing)
            subjectArray = subjectArray.concat(existing);

        if (subjectArray.length > 0){
            subjectArray = Ext4.unique(subjectArray);
            subjectArray.sort();
        }

        this.down('#subjArea').setValue(null);

        if (subjectArray.length)
            this.tabbedReportPanel.setSubjGrid(subjectArray);

        return subjectArray || [];
    },

    getFilters: function(){
        var subj = this.down('#subjArea').getValue();
        if (subj){
            subj = subj.split(';');
        }
        else {
            subj = [];
        }

        var otherSubjects = this.tabbedReportPanel.getSubjects();

        if (otherSubjects && otherSubjects.length)
            subj = subj.concat(otherSubjects);

        subj = Ext4.unique(subj);
        return {
            subjects: subj
        }
    },

    getTitle: function(){
        var otherSubjects = this.tabbedReportPanel.getSubjects();
        var subjects = this.getSubjects(otherSubjects);
        if (subjects && subjects.length){
            return subjects.join(', ');
        }

        return '';
    },

    checkValid: function(){
        var otherSubjects = this.tabbedReportPanel.getSubjects();
        var subjects = this.getSubjects(otherSubjects);

        if (!subjects.length){
            Ext4.Msg.alert('Error', 'Must enter at least one subject ID');
            return false;
        }

        return true;
    }
});