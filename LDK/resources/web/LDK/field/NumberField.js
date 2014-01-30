/**
 * Extension of Ext's numberfield designed to remove some undesired behavior,
 * mostly around keynav
 */
Ext4.define('LDK.form.field.Number', {
    extend: 'Ext.form.field.Number',
    alias: 'widget.ldk-numberfield',

    hideTrigger: true,
    keyNavEnabled: false,
    spinUpEnabled: false,
    spinDownEnabled: false
});