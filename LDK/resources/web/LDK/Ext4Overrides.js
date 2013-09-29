/**
 * This was added to allow configs for typeahead to be case-insensitive, and/or permit contains vs. startswith
 * These are controlled using the combo config properties anyMatch or caseSensitive
 */
Ext4.override(Ext4.form.field.ComboBox, {
    //this is overriden to allow the combo to be reset even when forceSelection=true
    assertValue: function() {
        var me = this,
                value = me.getRawValue(),
                rec, currentValue;

        if (me.forceSelection && !Ext4.isEmpty(value)) {
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
                    currentValue = me.value;
                    // Prevent an issue where we have duplicate display values with
                    // different underlying values.
                    if (!me.findRecordByValue(currentValue)) {
                        me.select(rec, true);
                    }
                } else {
                    me.setValue(me.lastSelection);
                }
            }
        }
        me.collapse();
    }
});