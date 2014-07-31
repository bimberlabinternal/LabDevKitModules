/**
 * Extension of Ext's numberfield designed to remove some undesired behavior,
 * mostly around keynav
 */
Ext4.define('LDK.form.field.Integer', {
    extend: 'LDK.form.field.Number',
    alias: 'widget.ldk-integerfield',

    allowDecimals: false
});
