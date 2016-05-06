Ext4.define('LDK.panel.LdapSettingsPanel', {
    extend: 'Ext.form.Panel',
    alias: 'widget.ldk-ldapsettingspanel',

    initComponent: function(){
        Ext4.QuickTips.init();

        Ext4.apply(this, {
            border: false,
            style: 'padding: 5px;',
            defaults: {
                border: false
            },
            items: [{
                html: 'This is an extension to LabKey Server produced by staff at Oregon Health and Science University, and is not officially supported by LabKey.  It is designed to provide a mechanism to automatically sync users and groups from an external LDAP server with users/groups in LabKey.  This sync is unidirectional, meaning changes in the LDAP server will be reflected in LabKey, but not the reverse.  It is designed to allow flexibility; however, it is still relatively new and has only been used on a small number of LDAP servers.' +
                    'Because the sync needs to perform searches against the LDAP server, rather just just authenticate, it operates somewhat differently than LabKey\'s built-in LDAP authentication module.  As such, there is relatively little shared configuration between the two, and enabling one does not currently enable the other.',
                style: 'padding-bottom: 20px;'
            },{
                html: 'To help manage users, <a href="' + LABKEY.ActionURL.buildURL('query', 'executeQuery', 'Shared', {schemaName: 'ldk', queryName: 'ldapSyncDiscrepancies'}) + '">click here</a> to view a list of users present in LABKEY, but not synced via LDAP, or records of users previously synced from LDAP that no longer have a LABKEY user.  The latter is common if the LDAP record was deactivated.  Because users can be created in LabKey outside of the LDAP sync this is not inherently a problem, but this report should provide information that may help with auditing or managing deactivated users.  This report does not take your site\'s domain name into account; however, you can filter the email address column using a contains filter.',
                style: 'padding-bottom: 20px;'
            },{
                xtype: 'panel',
                itemId: 'serverSettings',
                defaults: {
                    border: false
                },
                items: [{
                    html: 'Loading...'
                }]
            }]
        });

        this.callParent();

        LABKEY.Ajax.request({
            url : LABKEY.ActionURL.buildURL('ldk', 'getLdapSettings'),
            method : 'GET',
            success: LABKEY.Utils.getCallbackWrapper(this.onLoad, this),
            failure: LDK.Utils.getErrorCallback()
        });

        this.on('render', function(panel){
            if (!panel.ldapSettings){
                panel.mask('Loading...');
            }
        });
    },

    onLoad: function(results){
        this.ldapSettings = results;

        this.down('#serverSettings').removeAll();
        this.add({
            style: 'padding-bottom: 20px;',
            defaults: {
                border: false
            },
            items: [{
                html: '<b>Connection Settings</b>',
                style: 'padding-bottom: 10px;'
            },{
                html: 'The sync expects you to provide a service account, which will be used to authenticate all LDAP searches.  By design, most critical settings should be placed in the Tomcat config file (typically named labkey.xml or ROOT.xml).  This is the same location your server stored the DB credentials.  In this file, you should add a block similar to the following:<br><br>' +
                        '<pre><code>' + Ext4.String.htmlEncode(
                        '\t<Resource name="ldap/ConfigFactory" auth="Container"\n' +
                                '\t\ttype="org.labkey.ldk.ldap.LdapConnectionConfigFactory"\n' +
                                '\t\tfactory="org.labkey.ldk.ldap.LdapConnectionConfigFactory"\n' +
                                '\t\thost="ldap.yourServer.edu"\n' +
                                '\t\tport="389"\n' +
                                '\t\tprincipal="cn=serviceUser,dc=ldap,dc=yourServer,dc=edu"\n' +
                                '\t\tcredentials="password"\n' +
                                '\t\tuseSSL="true"\n' +
                                '\t\tsslProtocol="TLS"\n' +
                                '\t\t/>\n') +
                        '</code></pre><br><br>' +
                        'You should replace host, port, principal and credentials with values that match your server.  useSSL will default to false, and can be omitted.  port will default to 389, or 636 is using SSL.  ',
                style: 'padding-top: 10px;'
            },{
                style: 'padding-bottom: 10px;',
                html: 'Once you have configured tomcat, click the button below to attempt to authenticate.  Note: this is a very minimal test, which determines whether the host and credentials supplied in the tomcat config are able to authenticate against that server.'
            },{
                xtype: 'button',
                border: true,
                text: 'Test Connection',
                handler: function(btn){
                    Ext4.Msg.wait('Testing Connection...');

                    LABKEY.Ajax.request({
                        url : LABKEY.ActionURL.buildURL('ldk', 'testLdapConnection'),
                        method : 'GET',
                        success: LABKEY.Utils.getCallbackWrapper(function(response){
                            Ext4.Msg.hide();
                            Ext4.Msg.alert('Success', 'Authentication was successful');
                        }, this),
                        failure: function(responseObj, exception){
                            var msg = LABKEY.Utils.getMsgFromError(responseObj, exception, {
                                showExceptionClass: false,
                                msgPrefix: 'Unable to connect.  The error message was: '
                            });
                            Ext4.Msg.hide();
                            Ext4.Msg.alert('Failure', msg);
                        }
                    });
                }
            },{
                html: '<b>Search Strings (optional, but probably required)</b>',
                style: 'padding-bottom: 10px;padding-top: 20px;'
            },{
                html: 'In addition to the credentials, you can enter additional search strings that will be used when querying users or groups.  If you do not have experience with these, I recommend installing Apache Directory Studio or a similar LDAP browser first.  From this tool, connect to your LDAP server using the same credentials you entered into the tomcat config.  Attempt to perform a search using these strings and verify a result is returned.  Examples of search strings are:<br><br>' +
                    'Base search string: "DC=ldap,DC=myserver,DC=edu"<br>' +
                    'Group search string: "ou=My Department"<br>' +
                    'User search string: "ou=User Accounts".<br><br>' +
                    'User search filter: "(!(userAccountControl:1.2.840.113556.1.4.803:=2))".<br><br>' +
                    'When searching for groups, the base string will be concatenated with the user string to produce "ou=My Department,DC=ldap,DC=myserver,DC=edu".  This means that the search is limited to the domain ldap.myserver.edu and users will only be returned who are members of the organizational unit "My Department"<br><br>' +
                    'When searching for users, the base string will be concatenated with the user string to produce "ou=User Accounts,DC=ldap,DC=myserver,DC=edu".  This means that the search is limited to the domain ldap.myserver.edu and users will only be returned who are members of the organizational unit "User Accounts".  The filter string above is often used top limit to active accounts only.<br>',

                style: 'padding-bottom: 10px;'
            },{
                xtype: 'form',
                itemId: 'settingsForm',
                style: 'padding-bottom: 10px;',
                fieldDefaults: {
                    labelWidth: 160,
                    width: 600
                },
                items: [{
                    xtype: 'textfield',
                    fieldLabel: 'Base Search String',
                    itemId: 'baseSearchString',
                    name: 'baseSearchString',
                    value: this.ldapSettings.baseSearchString
                },{
                    xtype: 'textfield',
                    fieldLabel: 'Group Search String',
                    itemId: 'groupSearchString',
                    name: 'groupSearchString',
                    value: this.ldapSettings.groupSearchString
                },{
                    xtype: 'textfield',
                    fieldLabel: 'Group Filter String',
                    itemId: 'groupFilterString',
                    name: 'groupFilterString',
                    value: this.ldapSettings.groupFilterString
                },{
                    xtype: 'textfield',
                    fieldLabel: 'User Search String',
                    itemId: 'userSearchString',
                    name: 'userSearchString',
                    value: this.ldapSettings.userSearchString
                },{
                    xtype: 'textfield',
                    fieldLabel: 'User Filter String',
                    itemId: 'userFilterString',
                    name: 'userFilterString',
                    value: this.ldapSettings.userFilterString
                }]
            },{
                html: '<b>Field Mapping</b>',
                style: 'padding-bottom: 10px;'
            },{
                html: 'When users and groups are synced from the LDAP server, values from the LDAP entry will be used to populate fields in LabKey such as the email, displayName, etc.  You can customize which LDAP field will be used to populate each of these.',
                style: 'padding-bottom: 10px;'
            },{
                xtype: 'form',
                itemId: 'fieldMappingForm',
                style: 'padding-bottom: 10px;',
                items: this.getFieldMappingFormItems()
            },{
                html: '<b>Sync Behavior</b>',
                style: 'padding-bottom: 10px;'
            },{
                itemId: 'syncBehaviorForm',
                defaults: {
                    labelAlign: 'top'
                },
                bodyStyle: 'padding-bottom: 20px;',
                items: [{
                    xtype: 'radiogroup',
                    fieldLabel: 'Read userAccountControl attribute to determine if active?',
                    columns: 1,
                    itemId: 'userAccountControlBehavior',
                    defaults: {
                        name: 'userAccountControlBehavior'
                    },
                    items: [{
                        boxLabel: 'Yes',
                        inputValue: 'true',
                        checked: true
                    },{
                        boxLabel: 'No',
                        inputValue: 'false'
                    }]
                },{
                    xtype: 'radiogroup',
                    columns: 1,
                    fieldLabel: 'When A User Is Deleted From LDAP',
                    itemId: 'userDeleteBehavior',
                    defaults: {
                        name: 'userDeleteBehavior'
                    },
                    items: [{
                        boxLabel: 'Deactivate User From LabKey',
                        inputValue: 'deactivate',
                        checked: true
                    },{
                        boxLabel: 'Delete User From LabKey',
                        inputValue: 'delete'
                    }]
                },{
                    xtype: 'radiogroup',
                    columns: 1,
                    fieldLabel: 'When A Group Is Deleted From LDAP',
                    itemId: 'groupDeleteBehavior',
                    defaults: {
                        name: 'groupDeleteBehavior'
                    },
                    items: [{
                        boxLabel: 'Delete Group From LabKey',
                        inputValue: 'delete',
                        checked: true
                    },{
                        boxLabel: 'Do Nothing',
                        inputValue: 'doNothing'
                    }]
                },{
                    xtype: 'radiogroup',
                    columns: 1,
                    fieldLabel: 'Group Membership Sync Method',
                    itemId: 'memberSyncMode',
                    defaults: {
                        name: 'memberSyncMode'
                    },
                    items: [{
                        boxLabel: 'Remove all members from the LabKey group that are not present in the corresponding LDAP group',
                        inputValue: 'mirror',
                        checked: true
                    },{
                        boxLabel: 'Remove all LDAP users from the LabKey group that are not present in the corresponding LDAP group (this allows non-LDAP users to be added within LabKey)',
                        inputValue: 'removeDeletedLdapUsers'
                    },{
                        boxLabel: 'Do Nothing',
                        inputValue: 'noAction'
                    }]
                },{
                    xtype: 'radiogroup',
                    columns: 1,
                    fieldLabel: 'Set the LabKey user\'s information (name, email, etc), based on LDAP.  This will overwrite any changes made in LabKey',
                    itemId: 'userInfoChangedBehavior',
                    defaults: {
                        name: 'userInfoChangedBehavior'
                    },
                    items: [{
                        boxLabel: 'Yes',
                        inputValue: 'true',
                        checked: true
                    },{
                        boxLabel: 'No',
                        inputValue: 'false'
                    }]
                    //TODO: what if the user email changes?
                }]
            },{
                html: '<b>Choose What to Sync</b>',
                style: 'padding-bottom: 10px;'
            },{
                itemId: 'syncModePanel',
                style: 'padding-bottom: 10px;',
                defaults: {
                    border: false
                },
                items: [{
                    xtype: 'radiogroup',
                    columns: 1,
                    itemId: 'syncMode',
                    items: [{
                        name: 'syncMode',
                        boxLabel: 'All Users (subject to filter strings above)',
                        inputValue: 'usersOnly',
                        checked: true
                    },{
                        name: 'syncMode',
                        boxLabel: 'All Users and Groups (subject to filter strings above)',
                        inputValue: 'usersAndGroups'
                    },{
                        name: 'syncMode',
                        boxLabel: 'Sync Only Specific Groups and Their Members',
                        inputValue: 'groupWhitelist'
//                    },{
//                        name: 'syncMode',
//                        boxLabel: 'Sync All Groups Except Selected Group',
//                        inputValue: 'groupBlacklist'
                    }],
                    listeners: {
                        scope: this,
                        change: function(field, val, oldVal){
                            var panel = field.up('#syncModePanel');
                            var groupSelection = panel.down('#groupSelection');
                            groupSelection.removeAll();

//                            if (val.syncMode == 'groupBlacklist'){
//                                groupSelection.add(this.getGroupSelectionCfg('exclude'))
//                            }
                            if (val.syncMode == 'groupWhitelist') {
                                groupSelection.add(this.getGroupSelectionCfg('include'))
                            }
                        }
                    }
                },{
                    itemId: 'groupSelection',
                    style: 'padding-bottom: 10px;'
                }]
            },{
                html: '<b>Schedule</b>',
                style: 'padding-bottom: 10px;'
            },{
                xtype: 'checkbox',
                labelWidth: 170,
                fieldLabel: 'Is Enabled?',
                name: 'enabled',
                value: true
            },{
                xtype: 'textfield',
                labelWidth: 170,
                width: 400,
                fieldLabel: 'LabKey User',
                name: 'labkeyAdminEmail',
                helpPopup: 'This is the name of a valid LabKey site admin account.  This account will not be used to communicate with the LDAP server and does not need to be defined there.  It will be used internally when saving the audit trail and performing other DB operations.'
            },{
                xtype: 'numberfield',
                name: 'frequency',
                labelWidth: 170,
                width: 400,
                fieldLabel: 'Sync Frequency (Hours)',
                minValue: 0,
                allowDecimals: false
            },{
                layout: 'hbox',
                style: 'margin-top: 20px',
                defaults: {
                    style: 'margin-right: 5px;margin-bottom: 10px;'
                },
                items: [{
                    xtype: 'button',
                    border: true,
                    text: 'Save All Settings On Page',
                    handler: function(btn){
                        btn.up('ldk-ldapsettingspanel').doSaveSettings();
                    }
                },{
                    xtype: 'button',
                    border: true,
                    text: 'Preview Sync',
                    handler: function(btn){
                        Ext4.Msg.confirm('Preview Sync', 'This will perform a mock LDAP sync using the last saved settings, which do not necessarily match the current settings on this page.  If you would like to test new settings, please save first.  Do you want to continue?', function(choice){
                            if (choice != 'yes')
                                return;

                            btn.up('ldk-ldapsettingspanel').doSync(true);
                        }, this);
                    }
                },{
                    xtype: 'button',
                    border: true,
                    text: 'Sync Now',
                    handler: function(btn){
                        Ext4.Msg.confirm('Perform LDAP Sync', 'This will sync with the LDAP server using the last saved settings, which do not necessarily match the current settings on this page.  If you would like to test new settings, please save first.  Do you want to continue?', function(choice){
                            if (choice != 'yes')
                                return;

                            btn.up('ldk-ldapsettingspanel').doSync(false);
                      }, this);
                    }
                }]
            }]
        });

        this.setFieldsFromValues(results);
        this.unmask();
    },

    doSync: function(forPreview){
        Ext4.Msg.wait('Performing sync using last saved settings');

        LABKEY.Ajax.request({
            url : LABKEY.ActionURL.buildURL('ldk', 'initiateLdapSync'),
            method : 'POST',
            params: {
                forPreview: forPreview
            },
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function(results){
                Ext4.Msg.hide();

                Ext4.create('Ext.window.Window', {
                    modal: true,
                    closeAction: 'destroy',
                    title: 'LDAP Sync Results',
                    style: 'padding: 5px;',
                    width: 900,
                    height: 600,
                    defaults: {
                        border: false
                    },
                    items: [{
                        html: 'Below are the messages generated by the sync.  ' +
                            (forPreview ? 'No changes were performed.<br><br>' + 'NOTE: Because users & groups were not actually created, we cannot completely simulate what will happen to group membership.  If the users/groups already existed the messages should be accurate, but the may not be for users/groups that need to be created.' : '')
                    },{
                        xtype: 'textarea',
                        width: 885,
                        height: 500,
                        value: results.messages.join('\n')
                    }],
                    buttons: [{
                        text: 'Close',
                        handler: function(btn){
                            btn.up('window').close();
                        }
                    }]
                }).show();
            }, this),
            failure: LDK.Utils.getErrorCallback()
        });
    },

    getGroupSelectionCfg: function(mode){
        this.loadGroups();

        this.groupStore = this.groupStore || Ext4.create('Ext.data.Store', {
            fields: ['name', 'dn', 'description', 'objectGUID'],
            proxy: {
                type: 'memory'
            }
        }) ;

        var cfg = {
            style: 'padding-bottom: 20px;padding-left: 10px',
            border: false,
            items: [{
                xtype: 'itemselector',
                border: true,
                height: 250,
                width: 800,
                autoScroll: true,
                labelAlign: 'top',
                displayField: 'name',
                valueField: 'dn',
                multiSelect: false,
                store: this.groupStore,
                buttons: ['add', 'remove']
            },{
                xtype: 'button',
                text: 'Reload Group List',
                handler: function(btn){
                    var panel = btn.up('ldk-ldapsettingspanel');
                    panel.groupStore.removeAll();
                    panel.loadGroups();

                }
            }]
        };

//        if (mode == 'exclude'){
//            Ext4.apply(cfg.items[0], {
//                itemId: 'disallowedDn',
//                name: 'disallowedDn',
//                fieldLabel: 'Include All Groups Except Those Listed Here',
//                value: (this.ldapSettings ? this.ldapSettings.disallowedDn : null)
//            });
//        }
        if (mode == 'include') {
            Ext4.apply(cfg.items[0], {
                itemId: 'allowedDn',
                name: 'allowedDn',
                fieldLabel: 'Choose Only Those Groups To Include',
                value: (this.ldapSettings ? this.ldapSettings.allowedDn : null)
            });
        }

        return cfg
    },

    onGroupsLoad: function(results){
        var toAdd = [];
        for (var i=0;i<results.groups.length;i++){
            toAdd.push(this.groupStore.createModel(results.groups[i]));
        }


        this.groupStore.add(toAdd);
        this.groupStore.sort('name');

        var field = this.down('itemselector');
        field.bindStore(this.groupStore)
    },

    loadGroups: function(callback, scope){
        Ext4.Msg.wait('Loading groups using last saved config...');

        LABKEY.Ajax.request({
            url : LABKEY.ActionURL.buildURL('ldk', 'listLdapGroups'),
            method : 'POST',
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function(results){
                Ext4.Msg.hide();
                this.onGroupsLoad(results);
            }, this),
            failure: LDK.Utils.getErrorCallback()
        });
    },

    getFieldMappingFormItems: function(){
        var fields = [{
            displayName: 'Email',
            itemId: 'emailFieldMapping',
            name: 'emailFieldMapping'
        },{
            displayName: 'Display Name',
            itemId: 'displayNameFieldMapping',
            name: 'displayNameFieldMapping'
        },{
            displayName: 'First Name',
            itemId: 'firstNameFieldMapping',
            name: 'firstNameFieldMapping'
        },{
            displayName: 'Last Name',
            itemId: 'lastNameFieldMapping',
            name: 'lastNameFieldMapping'
        },{
            displayName: 'Phone Number',
            itemId: 'phoneNumberFieldMapping',
            name: 'phoneNumberFieldMapping'
        },{
            displayName: 'UID',
            helpPopup: 'This should hold the value that uniquely identifies this record on the LDAP server.  Usually this would be the login, but it could also be the distinguishing name or objectId',
            itemId: 'uidFieldMapping',
            name: 'uidFieldMapping'
        }];


        var toAdd = [];
        Ext4.each(fields, function(field){
            toAdd.push({
                xtype: 'textfield',
                labelWidth: 120,
                width: 300,
                fieldLabel: field.displayName,
                helpPopup: field.helpPopup,
                name: field.name,
                value: null
            });
        }, this);

        return toAdd;
    },

    doSaveSettings: function(){
        var vals = this.getForm().getFieldValues();
        Ext4.Msg.wait('Saving...');

        LABKEY.Ajax.request({
            url : LABKEY.ActionURL.buildURL('ldk', 'setLdapSettings'),
            method : 'POST',
            params: vals,
            success: LABKEY.Utils.getCallbackWrapper(this.afterSaveSettings, this),
            failure: LDK.Utils.getErrorCallback()
        });
    },

    afterSaveSettings: function(results){
        Ext4.Msg.hide();
        Ext4.Msg.alert('Success', 'Settings saved');
    },

    setFieldsFromValues: function(results){
        this.ldapSettings = results;
        for (var prop in this.ldapSettings){
            var field = this.down('[name=' + prop + ']');
            if (field){
                if (field.isXType('radiogroup')){
                    var obj = {};
                    obj[field.name || field.itemId] = this.ldapSettings[prop];
                    field.setValue(obj);
                }
                else {
                    field.setValue(this.ldapSettings[prop]);
                }
            }
        }
    }

});