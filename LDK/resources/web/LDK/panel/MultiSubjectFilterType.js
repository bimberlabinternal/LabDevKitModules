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
        var subjs = this.getSubjects(this.tabbedReportPanel.getSubjects());
        this.tabbedReportPanel.setSubjGrid(false, Ext4.isDefined(this.aliasTable), subjs);
        this.clearSubjectArea();
    },

    prepareRemove: function(){
        this.tabbedReportPanel.setSubjGrid(true);
    },

    clearSubjectArea: function(){
        this.down('#subjArea').setValue(null);
    },
    
    getItems: function(){
        var ctx = this.filterContext || {};

        var toAdd = [];

        toAdd.push({
            width: 200,
            html: 'Enter ' + this.nounSingular + (Ext4.isDefined(this.multiSearchText)?
                    this.multiSearchText:' IDs:<br><div style="width: 175px;"><i>(Separated ' +
                    'by commas, semicolons, space or line breaks)</i></div>')
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
                    name: 'subjectIds',
                    value: Ext4.isArray(ctx.subjects) ? ctx.subjects.join(';') : ctx.subjects
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
                    text: 'Add',
                    handler: function(btn){
                        var panel = btn.up('ldk-multisubjectfiltertype');
                        panel.addId(panel.updateSubjects, panel);
                    }
                },{
                    text: 'Replace',
                    handler: function(btn){
                        var panel = btn.up('ldk-multisubjectfiltertype');
                        panel.addId(panel.replaceSubjects, panel);
                    }
                },{
                    text: 'Clear',
                    handler: function(btn){
                        var panel = btn.up('ldk-multisubjectfiltertype');
                        panel.tabbedReportPanel.setSubjGrid(true);
                        panel.down('#subjArea').setValue();
                        panel.subjects = [];
                        panel.getSubjects();
                    }
                }]
            }]
        });

        return toAdd;
    },

    updateSubjects: function () {
        this.renderSubjects(false, this.getSubjects());
    },

    replaceSubjects: function () {
        this.renderSubjects(true, this.getSubjects());
    },

    addId: function (callback, panel) {
        var subjectArray = LDK.Utils.splitIds(this.down('#subjArea').getValue(), false, false, this.caseInsensitive);

        if (subjectArray.length > 0) {
            subjectArray = Ext4.unique(subjectArray);
            subjectArray.sort();
        }

        this.down('#subjArea').setValue(null);

        this.subjects = subjectArray;

        if(Ext4.isDefined(this.aliasTable)) {
            this.aliases = {};
            this.getAlias(subjectArray, callback, panel);
        } else {
            callback.call(this);
        }
    },

    getAlias: function (subjectArray, callback, panel) {
        this.aliasTableConfig(subjectArray);

        this.aliasTable.success = function (results) {
            this.handleAliasResults(results);
            if (callback)
                callback.call(panel);
        };

        LABKEY.Query.selectRows(this.aliasTable);
    },

    handleAliases: function (results) {

        this.handleAliasResults(results);

        if (this.subjects.length)
            this.renderSubjects(false, this.getSubjects());
    },

    renderSubjects: function (clear, subjects) {
        this.down('#subjArea').setValue(null);
        this.tabbedReportPanel.setSubjGrid(clear, Ext4.isDefined(this.aliasTable), subjects, this.aliases, this.notFound);
    },

    getSubjects: function(existing){
        //we clean up, combine, then split the subjectBox and subject inputs
        var subjectArray = this.subjects;
        if (subjectArray.length == 0)
            subjectArray = LDK.Utils.splitIds(this.down('#subjArea').getValue(), false, false, this.caseInsensitive);

        if (existing)
            subjectArray = subjectArray.concat(existing);

        if (subjectArray.length > 0){
            subjectArray = Ext4.unique(subjectArray);
            subjectArray.sort();
        }

        this.subjects = subjectArray;
        return subjectArray || [];
    },

    getFilters: function(){
        var filters = this.subjects;
        var otherSubjects = this.tabbedReportPanel.getSubjects();

        if (otherSubjects && otherSubjects.length)
            filters = filters.concat(otherSubjects);

        return {
            subjects: Ext4.unique(filters)
        }
    },

    getFilterArray: function (tab) {
        return this.handleFilters(tab, this.getFilters().subjects);
    },

    handleReport: function(panel) {

        this.subjects = [];
        this.subjects = this.subjects.concat(panel.subjects[panel.btnTypes.subjects]);
        this.subjects = this.subjects.concat(panel.subjects[panel.btnTypes.aliases]);
        this.subjects = this.subjects.concat(panel.subjects[panel.btnTypes.conflicted]);
    },

    loadReport: function (tab, callback, panel, forceRefresh) {
        var subjectArray = LDK.Utils.splitIds(this.down('#subjArea').getValue(), false, false, this.caseInsensitive);

        if(subjectArray.length > 0) {
            this.addId(function(){
                this.updateSubjects();
                this.handleReport(panel);
                callback.call(panel, this.handleFilters(tab, this.subjects), forceRefresh);
            }, this);
        }
        else {
            this.handleReport(panel);
            callback.call(panel, this.handleFilters(tab, this.subjects), forceRefresh);
        }
    },

    getTitle: function(){
        var otherSubjects = this.tabbedReportPanel.getSubjects();
        var subjects = this.getSubjects(otherSubjects);
        if (subjects && subjects.length){
            if (subjects.length <= 6)
                return subjects.join(', ');

            return subjects.slice(0,5).join(', ') + '...';
        }

        return '';
    },

    isValid: function(){
        var otherSubjects = this.tabbedReportPanel.getSubjects();
        var subjects = this.getSubjects(otherSubjects);

        if (!subjects.length){
            return false;
        }

        return true;
    },

    getFilterInvalidMessage: function(){
        return 'Error', 'Must enter at least one valid ' + this.nounSingular + ' ID';
    }
});