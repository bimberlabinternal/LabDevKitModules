Ext4.define('LDK.panel.SingleSubjectFilterType', {
    extend: 'LDK.panel.AbstractFilterType',
    alias: 'widget.ldk-singlesubjectfiltertype',

    statics: {
        filterName: 'singleSubject',
        DEFAULT_LABEL: 'Single Subject'
    },

    nounSingular: 'Subject',

    // This flag updates alias/Id db query to use a case insensitive LK filter (CONTAINS). The
    // results are processed after to do case insensitive matches on the results with the user entered Ids.
    // This is primarily for use when an alias table is set for the filter.
    caseInsensitive: false,

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
                name: 'subjectId',
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
        if(tab.report) {
            subjectFieldName = tab.report.subjectFieldName;
        }
        else if(tab.items[0].report) {
            subjectFieldName = tab.items[0].report.subjectFieldName;
        }
        if (!subjectFieldName) {
            return filterArray;
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

    isValid: function () {
        var val = this.down('#subjArea').getValue();
        val = Ext4.String.trim(val);
        if (!val) {
            return false;
        }

        return true;
    },

    getFilterInvalidMessage: function(){
        return 'Must enter at least one ' + this.nounSingular + ' ID';
    },

    validateReportForFilterType: function (report) {
        if (!report.subjectFieldName)
            return 'This report cannot be used with the selected filter type, because the report does not contain a ' + this.nounSingular + ' Id field';

        if (this.subjects.length === 0)
            return 'Must enter at least one valid Subject ID.'

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
    
    loadReport: function(tab, callback, panel, forceRefresh){
        var subjectArray = LDK.Utils.splitIds(this.down('#subjArea').getValue());

        if (subjectArray.length > 0){
            subjectArray = Ext4.unique(subjectArray);
        }

        this.subjects = subjectArray;
        this.aliases = {};
        if (Ext4.isDefined(this.aliasTable)) {
            this.getAlias(subjectArray, callback, panel, tab, forceRefresh);
        }
        else {
            callback.call(panel, this.handleFilters(tab, this.subjects), forceRefresh);
        }
    },

    getSubjectMessages: function () {
        var msg = "";

        // Create message for aliases
        if (!Ext4.isEmpty(this.aliases)) {
            for (var alias in this.aliases) {
                if (this.aliases.hasOwnProperty(alias)) {
                    Ext4.each(this.aliases[alias], function (id) {
                        msg += "<div class='labkey-error'>Alias " + alias + " mapped to ID " + id;
                        if (this.subjects.indexOf(alias) !== -1) {
                            msg += " and is a real ID";
                        }
                        msg += "</div>";
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

        // When caseInsensitive is true, use contains filter to ensure case insensitivity across dbs. This will return more
        // results than necessary in some cases, however those results are filtered to match the user input and non-matching
        // results are not used.
        var filterType = this.caseInsensitive ? LABKEY.Filter.Types.CONTAINS_ONE_OF : LABKEY.Filter.Types.EQUALS_ONE_OF;
        var filterCol = this.aliasTable.aliasColumn ? this.aliasTable.aliasColumn : this.aliasTable.idColumn;
        this.aliasTable.filterArray = [LABKEY.Filter.create(filterCol, subjectArray.join(';'), filterType)];
        this.aliasTable.columns = this.aliasTable.idColumn + (Ext4.isDefined(this.aliasTable.aliasColumn) ? ',' + this.aliasTable.aliasColumn : '');
    },

    getAlias: function (subjectArray, callback, panel, tab, forceRefresh) {
        this.aliasTableConfig(subjectArray);

        this.aliasTable.success = function (results) {
            this.handleAliases(results);
            this.getSubjectMessages();
            callback.call(panel, this.handleFilters(tab, this.subjects), forceRefresh);
        };

        LABKEY.Query.selectRows(this.aliasTable);
    },

    // This looks for rows that match subjects case insensitively. This is called due to the filter in the case insensitive
    // case being "contains" instead of "equals" to get all possible casings. This also gets Ids that just contain the
    // subject id as a substring. Thus we need this to filter out the non-matching rows.
    getCaseInsensitiveMatches: function(results) {
        var hasAlias = !!this.aliasTable.aliasColumn;
        var updatedResults = [];

        Ext4.each(results.rows, function (row) {
            if (hasAlias) {
                var rowAlias = row[this.aliasTable.aliasColumn];

                var aliasIndex = this.subjects.indexOf(rowAlias);
                if (aliasIndex === -1) {
                    for (var i = 0; i < this.subjects.length; i++) {
                        if (rowAlias.toLowerCase() === this.subjects[i].toString().toLowerCase()) {
                            aliasIndex = i;
                            break;
                        }
                    }
                }

                if (aliasIndex !== -1) {
                    updatedResults.push(row);
                }
            }
            else {
                var rowId = row[this.aliasTable.idColumn];

                var rowIndex = this.subjects.indexOf(rowId);
                if (rowIndex === -1) {
                    for (var i = 0; i < this.subjects.length; i++) {
                        if (rowId.toLowerCase() === this.subjects[i].toString().toLowerCase()) {
                            rowIndex = i;
                            break;
                        }
                    }
                }

                if (rowIndex !== -1) {
                    updatedResults.push(row);
                }
            }
        }, this)
        return updatedResults;
    },


    handleAliasResults: function (results) {
        this.notFound = Ext4.clone(this.subjects);

        var rows = this.caseInsensitive ? this.getCaseInsensitiveMatches(results) : results.rows;
        var updatedSubjects = [];

        Ext4.each(rows, function (row) {

            var rowId = row[this.aliasTable.idColumn];
            updatedSubjects.push(rowId);

            if (this.aliasTable.aliasColumn) {

                var rowAlias = row[this.aliasTable.aliasColumn];

                // Remove from notFound array if found
                var subjIndex = this.notFound.indexOf(rowAlias);
                if (subjIndex === -1 && this.caseInsensitive) {
                    for (var i = 0; i < this.notFound.length; i++) {
                        if (rowAlias.toLowerCase() === this.notFound[i].toString().toLowerCase()) {
                            subjIndex = i;
                            break;
                        }
                    }
                }

                if (subjIndex !== -1) {
                    this.notFound.splice(subjIndex, 1);
                }

                // Resolve aliases
                if (rowId !== rowAlias) {

                    var aliasList = this.aliases[rowAlias];
                    if (aliasList) {
                        aliasList.push(rowId);
                    }
                    else {
                        this.aliases[rowAlias] = [rowId];
                    }
                }
            }
            else {
                // Remove from notFound array if found
                var idIndex = this.notFound.indexOf(rowId);
                if (idIndex === -1 && this.caseInsensitive) {
                    for (var i = 0; i < this.notFound.length; i++) {
                        if (rowId.toLowerCase() === this.notFound[i].toString().toLowerCase()) {
                            idIndex = i;
                            break;
                        }
                    }
                }

                // TODO: Update this and LDK.Utils.splitIds when the case sensitive cache issues are fixed
                if (idIndex === -1) {
                    for (var nfIndex = 0; nfIndex < this.notFound.length; nfIndex++) {
                        if (this.notFound[nfIndex].toString().toUpperCase() === rowId) {
                            idIndex = nfIndex;
                            break;
                        }
                    }
                }

                if (idIndex !== -1) {
                    this.notFound.splice(idIndex, 1);
                }
            }
        }, this);

        // Remove any not found
        Ext4.each(this.notFound, function (id) {
            var found = updatedSubjects.indexOf(id);
            if (found !== -1)
                updatedSubjects.splice(found, 1);
        }, this);

        this.subjects = Ext4.unique(updatedSubjects);
        this.subjects.sort();
    },

    handleAliases: function (results) {

        this.handleAliasResults(results);
        this.down('#subjArea').setValue(this.subjects.join(';'));
    }
});