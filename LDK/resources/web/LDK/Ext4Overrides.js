/**
 * This was added to allow configs for typeahead to be case-insensitive, and/or permit contains vs. startswith
 * These are controlled using the combo config properties anyMatch or caseSensitive
 */
Ext4.override(Ext4.form.field.ComboBox, {
    doQuery: function(queryString, forceAll, rawQuery) {
        queryString = queryString || '';

        // store in object and pass by reference in 'beforequery'
        // so that client code can modify values.
        var me = this,
                qe = {
                    query: queryString,
                    forceAll: forceAll,
                    combo: me,
                    cancel: false
                },
                store = me.store,
                isLocalMode = me.queryMode === 'local',
                needsRefresh;

        if (me.fireEvent('beforequery', qe) === false || qe.cancel) {
            return false;
        }

        // get back out possibly modified values
        queryString = qe.query;
        forceAll = qe.forceAll;

        // query permitted to run
        if (forceAll || (queryString.length >= me.minChars)) {
            // expand before starting query so LoadMask can position itself correctly
            me.expand();

            // make sure they aren't querying the same thing
            if (!me.queryCaching || me.lastQuery !== queryString) {
                me.lastQuery = queryString;

                if (isLocalMode) {
                    // forceAll means no filtering - show whole dataset.
                    store.suspendEvents();
                    needsRefresh = me.clearFilter();
                    if (queryString || !forceAll) {
                        //NOTE: added by BNB to support contains filtering
                        me.anyMatch = me.anyMatch === undefined? false : me.anyMatch;
                        me.caseSensitive = me.caseSensitive === undefined? false : me.caseSensitive;

                        me.activeFilter = new Ext4.util.Filter({
                            root: 'data',
                            property: me.displayField,
                            value: me.enableRegEx ? new RegExp(queryString) : queryString,
                            anyMatch: me.anyMatch,
                            caseSensitive: me.caseSensitive
                        });

                        store.filter(me.activeFilter);
                        needsRefresh = true;
                    } else {
                        delete me.activeFilter;
                    }
                    store.resumeEvents();
                    if (me.rendered && needsRefresh) {
                        me.getPicker().refresh();
                    }
                } else {
                    // Set flag for onLoad handling to know how the Store was loaded
                    me.rawQuery = rawQuery;

                    // In queryMode: 'remote', we assume Store filters are added by the developer as remote filters,
                    // and these are automatically passed as params with every load call, so we do *not* call clearFilter.
                    if (me.pageSize) {
                        // if we're paging, we've changed the query so start at page 1.
                        me.loadPage(1);
                    } else {
                        store.load({
                            params: me.getParams(queryString)
                        });
                    }
                }
            }

            // Clear current selection if it does not match the current value in the field
            if (me.getRawValue() !== me.getDisplayValue()) {
                me.ignoreSelection++;
                me.picker.getSelectionModel().deselectAll();
                me.ignoreSelection--;
            }

            if (isLocalMode) {
                me.doAutoSelect();
            }
            if (me.typeAhead) {
                me.doTypeAhead();
            }
        }
        return true;
    },

    //this is overriden to allow the combo to be reset even when forceSelection=true
    assertValue: function() {
        var me = this,
                value = me.getRawValue(),
                rec;

        if (me.forceSelection && (!Ext4.isEmpty(value) || !me.allowBlank)) {
            if (me.multiSelect) {
                // For multiselect, check that the current displayed value matches the current
                // selection, if it does not then revert to the most recent selection.
                if (value !== me.getDisplayValue()) {
                    me.setValue(me.lastSelection);
                }
            } else {
                // For single-select, match the displayed value to a record and select it,
                // if it does not match a record then revert to the most recent selection.
                rec = me.findRecordByDisplay(value);
                if (rec) {
                    me.select(rec);
                } else {
                    me.setValue(me.lastSelection);
                }
            }
        }
        me.collapse();
    }
});