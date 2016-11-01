Ext4.define('LDK.panel.SingleSubjectFilterType', {
    extend: 'LDK.panel.AbstractFilterType',
    alias: 'widget.ldk-singlesubjectfiltertype',

    statics: {
        filterName: 'singleSubject',
        DEFAULT_LABEL: 'Single Subject'
    },

    nounSingular: 'Subject',

    subjects: [],
    notFound: [],
    aliases: {},

    initComponent: function () {
        this.items = this.getItems();
        this.callParent();
    },

    getItems: function () {
        var ctx = this.filterContext;
        var toAdd = [];

        toAdd.push({
            width: 200,
            html: 'Enter ' + this.nounSingular + ' Id:',
            style: 'margin-bottom:10px'
        });

        toAdd.push({
            xtype: 'panel',
            items: [{
                xtype: 'textfield',
                width: 165,
                itemId: 'subjArea',
                value: Ext4.isArray(ctx.subjects) ? ctx.subjects.join(';') : ctx.subjects,
                listeners: {
                    scope: this,
                    render: function (field) {
                        field.keyListener = this.tabbedReportPanel.createKeyListener(field.getEl());
                    }
                }
            }]
        });

        return toAdd;
    },

    getFilterArray: function (tab) {
        return this.handleFilters(tab, this.subjects);
    },

    handleFilters: function (tab, filters) {
        var filterArray = {
            subjects: LDK.Utils.splitIds(this.down('#subjArea').getValue()),
            removable: [],
            nonRemovable: []
        };

        var subjectFieldName;
        if(tab.report)
            subjectFieldName= tab.report.subjectFieldName;
        if (!subjectFieldName) {
            return;
        }

        if (filters && filters.length) {
            filterArray.subjects = Ext4.unique(filterArray.subjects.concat(filters)).sort();
            if (filters.length == 1)
                filterArray.nonRemovable.push(LABKEY.Filter.create(subjectFieldName, filters[0], LABKEY.Filter.Types.EQUAL));
            else
                filterArray.nonRemovable.push(LABKEY.Filter.create(subjectFieldName, filters.join(';'), LABKEY.Filter.Types.EQUALS_ONE_OF));
        }

        return filterArray;
    },

    checkValid: function () {
        var val = this.down('#subjArea').getValue();
        val = Ext4.String.trim(val);
        if (!val) {
            Ext4.Msg.alert('Error', 'Must enter at least one ' + this.nounSingular + ' ID');
            return false;
        }

        return true;
    },

    validateReport: function (report) {
        if (!report.subjectFieldName)
            return 'This report cannot be used with the selected filter type, because the report does not contain a ' + this.nounSingular + ' Id field';

        return null;
    },

    getTitle: function () {
        if (this.subjects && this.subjects.length) {
            if (this.subjects.length <= 6)
                return this.subjects.join(', ');

            return this.subjects.slice(0, 5).join(', ') + '...';
        }

        return '';
    },
    
    loadReport: function(tab, callback, panel){
        var subjectArray = LDK.Utils.splitIds(this.down('#subjArea').getValue());

        if (subjectArray.length > 0){
            subjectArray = Ext4.unique(subjectArray);
        }

        this.subjects = subjectArray;
        this.aliases = {};
        if (Ext4.isDefined(this.aliasTable)) {
            this.getAlias(subjectArray, callback, panel, tab);
        }
        else {
            callback.call(panel, this.handleFilters(tab, this.subjects));
        }
    },

    getSubjectMessages: function () {
        var msg = "";

        // Create message for aliases
        if (!Ext4.isEmpty(this.aliases)) {
            for (var alias in this.aliases) {
                if (this.aliases.hasOwnProperty(alias)) {
                    Ext4.each(this.aliases[alias], function (id) {
                        msg += "<div class='labkey-error'>Alias " + alias + " mapped to ID " + id + "</div>";
                    }, this);
                }
            }
        }

        // Create messages for not found ids
        if (!Ext4.isEmpty(this.notFound)) {
            Ext4.each(this.notFound, function (id) {
                msg += "<div class='labkey-error'>ID " + id + " not found.</div>";
            }, this);
        }
        this.tabbedReportPanel.setSubjMsg(msg);
    },

    aliasTableConfig: function (subjectArray) {
        this.aliasTable.scope = this;
        this.aliasTable.filterArray = [LABKEY.Filter.create('alias', subjectArray.join(';'), LABKEY.Filter.Types.EQUALS_ONE_OF)];
        this.aliasTable.columns = this.aliasTable.idColumn + (Ext4.isDefined(this.aliasTable.aliasColumn) ? ',' + this.aliasTable.aliasColumn : '');
    },

    getAlias: function (subjectArray, callback, panel, tab) {
        this.aliasTableConfig(subjectArray);

        this.aliasTable.success = function (results) {
            this.handleAliases(results);
            this.getSubjectMessages();
            callback.call(panel, this.handleFilters(tab, this.subjects));
        };

        LABKEY.Query.selectRows(this.aliasTable);
    },

    handleAliasResults: function (results) {
        this.notFound = Ext4.clone(this.subjects);
        Ext4.each(results.rows, function (row) {

            if (this.aliasTable.aliasColumn) {

                // Remove from notFound array if found
                var subjIndex = this.notFound.indexOf(row[this.aliasTable.aliasColumn]);
                if (subjIndex != -1) {
                    this.notFound.splice(subjIndex, 1);
                }

                // Resolve aliases
                if (row[this.aliasTable.idColumn] != row[this.aliasTable.aliasColumn]) {
                    var index = this.subjects.indexOf(row[this.aliasTable.aliasColumn]);
                    if (index != -1) {
                        this.aliases[row[this.aliasTable.aliasColumn]] = [row[this.aliasTable.idColumn]];
                        this.subjects.splice(index, 1, row[this.aliasTable.idColumn]);
                    }
                    // In case an alias matches multiple ID's
                    else {
                        for (var alias in this.aliases) {
                            if (this.aliases.hasOwnProperty(alias) && row[this.aliasTable.aliasColumn] == alias) {
                                this.aliases[row[this.aliasTable.aliasColumn]].push(row[this.aliasTable.idColumn]);
                                index = this.subjects.indexOf(this.aliases[row[this.aliasTable.aliasColumn]][0]);
                                this.subjects.splice(index, 0, row[this.aliasTable.idColumn]);
                            }
                        }
                    }
                }
            }
            else {
                // Remove from notFound array if found
                var idIndex = this.notFound.indexOf(row[this.aliasTable.idColumn]);

                // TODO: Update this and LDK.Utils.splitIds when the case sensitive cache issues are fixed
                if (idIndex == -1) {
                    for (var nfIndex = 0; nfIndex < this.notFound.length; nfIndex++) {
                        if (this.notFound[nfIndex].toUpperCase() == row[this.aliasTable.idColumn]) {
                            idIndex = nfIndex;
                            break;
                        }
                    }
                }

                if (idIndex != -1) {
                    this.notFound.splice(idIndex, 1);
                }
            }
        }, this);

        // Remove any not found
        Ext4.each(this.notFound, function (id) {
            var found = this.subjects.indexOf(id);
            if (found != -1)
                this.subjects.splice(found, 1);
        }, this);

        this.subjects = Ext4.unique(this.subjects);
        this.subjects.sort();
    },

    handleAliases: function (results) {

        this.handleAliasResults(results);
        this.down('#subjArea').setValue(this.subjects.join(';'));
    }
});