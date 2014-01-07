Ext4.define('LDK.panel.NotificationAdminPanel', {
    extend: 'Ext.form.Panel',

    initComponent: function(){
        Ext4.apply(this, {
            border: false
        });

        Ext4.QuickTips.init();

        this.callParent();

        LDK.Utils.getNotificationDetails({
            scope: this,
            failure: LDK.Utils.getErrorCallback(),
            success: this.onLoad
        });
    },

    onLoad: function(results){
        var notifications = results.notifications;
        notifications = LDK.Utils.sortByProperty(notifications, 'name', false);

        var notificationMap = {};

        for (var i=0;i<notifications.length;i++){
            var notification = notifications[i];

            if (!notification.available)
                continue;

            if (!notificationMap[notification.category])
                notificationMap[notification.category] = [];

            var notificationItems = notificationMap[notification.category];
            var timeSince = notification.lastRun == 0 ? null : ((new Date()) - notification.lastRun) / (1000 * 60);

            notificationItems.push({
                layout: 'hbox',
                border: false,
                style: 'padding-bottom: 10px;',
                defaults: {
                    border: false,
                    style: 'padding: 5px;'
                },
                itemId: notification.name,
                items: [{
                    html: notification.name,
                    cls: 'ldk-notificationlabel',
                    width: 200
                },{
                    width: 350,
                    html: ['Schedule: ' + notification.schedule,
                        'Last Run: ' + (notification.lastRun == 0 ? 'Never' : new Date(notification.lastRun).format('Y-m-d H:i')),
                        'Next Fire Time: ' + (notification.nextFireTime ? new Date(notification.nextFireTime).format('Y-m-d H:i') : ''),
                        'Time Since: ' + (notification.durationString ? notification.durationString : ''),
                        'Description: ' + (notification.description ? notification.description: ''),
                        'You are ' + (notification.subscriptions.length ? '' : 'not ') +  'subscribed to this notification'
                    ].join('<br>')
                },{
                    xtype: 'combo',
                    editable: false,
                    width: 120,
                    style:'margin: 5px;',
                    displayField: 'status',
                    valueField: 'status',
                    store: Ext4.create('Ext.data.ArrayStore', {
                        fields: ['status', 'rawValue'],
                        data: [
                            ['Enabled', true],
                            ['Disabled', false]
                        ]
                    }),
                    dataIndex: 'active',
                    notification: notification.key,
                    disabled: !LABKEY.Security.currentUser.isAdmin || !notification.available,
                    value: notification.active ? 'Enabled' : 'Disabled'
                },{
                    layout: 'vbox',
                    items: [{
                        xtype: 'ldk-linkbutton',
                        text: 'Run Report In Browser',
                        target: '_self',
                        linkCls: 'labkey-text-link',
                        href: LABKEY.ActionURL.buildURL('ldk', 'runNotification', null, {key: notification.key})
                    },{
                        xtype: 'ldk-linkbutton',
                        text: 'Manually Trigger Email',
                        linkCls: 'labkey-text-link',
                        notificationKey: notification.key,
                        handler: function(btn){
                            Ext4.Msg.confirm('Send Email', 'You are about to manually trigger this notification email to send, which will go to all subscribed users.  Are you sure you want to do this?', function(val){
                                if (val == 'yes'){
                                    LABKEY.Ajax.request({
                                        url: LABKEY.ActionURL.buildURL('ldk', 'sendNotification', null, {key: btn.notificationKey}),
                                        failure: LDK.Utils.getErrorCallback()
                                    });
                                }
                            }, this);
                        }
                    },{
                        xtype: 'ldk-linkbutton',
                        text: 'Manage Subscribed Users/Groups',
                        target: '_self',
                        linkCls: 'labkey-text-link',
                        notification: notification,
                        hidden: !LABKEY.Security.currentUser.isAdmin,
                        handler: function(btn){
                            Ext4.create('LDK.window.ManageNotificationWindow', {
                                notification: btn.notification
                            }).show(btn);
                        }
                    }]
                }]
            });
        }

        notificationItems = [];
        if (!notifications.length){
            notificationItems.push({
                border: false,
                style: 'padding-bottom: 10px;',
                html: 'There are no notifications available in this folder'
            });
        }

        var keys = Ext4.Object.getKeys(notificationMap);
        keys = keys.sort();
        Ext4.each(keys, function(key){
            var items = [{
                html: '<b>Notifications: ' + key + '</b>',
                style: 'padding-bottom: 5px;padding-top: 20px;',
                border: false
            }];

            items.push({
                border: true,
                items: notificationMap[key]
            });

            notificationItems.push({
                border: false,
                items: items
            });
        });

        this.add({
            xtype: 'form',
            border: false,
            bodyStyle: 'padding: 5px;',
            defaults: {
                border: false
            },
            fieldDefaults: {
                labelWidth: 140,
                width: 400
            },
            listeners: {
                render: function(panel){
                    panel.getForm().isValid();
                }
            },
            items: [(results.serviceEnabled ? {} : {
                html: '<b>NOTE: The notification service has been disabled at the site level, which prevents any automatic emails from being sent.  If you believe this is an error, please contact your site admin and ask them to enable the notification service through the admin console.</b>',
                style: 'padding-bottom: 20px;',
                border: false
            }),{
                html: '<b>General Settings:</b>',
                style: 'padding-bottom: 5px;',
                hidden: !LABKEY.Security.currentUser.isAdmin,
                border: false
            },{
                xtype: 'textfield',
                fieldLabel: 'Notification User',
                tabIndex: 1,
                allowBlank: false,
                helpPopup: 'This is the LabKey user that will be used to execute queries and send the emails.  It must be an admin in this folder.',
                dataIndex: 'user',
                hidden: !LABKEY.Security.currentUser.isAdmin,
                vtype: 'email',
                value: results.notificationUser
            },{
                xtype: 'textfield',
                fieldLabel: 'Reply Email',
                tabIndex: 1,
                allowBlank: false,
                hidden: !LABKEY.Security.currentUser.isAdmin,
                helpPopup: 'This will be used as the reply email for all sent messages.',
                dataIndex: 'replyEmail',
                vtype: 'email',
                value: results.replyEmail
            },{
                xtype: 'panel',
                border: false,
                itemId: 'notificationSection',
                items: notificationItems
            }],
            dockedItems: [{
                xtype: 'toolbar',
                dock: 'bottom',
                ui: 'footer',
                style: 'background: transparent;',
                items: [{
                    text: 'Save',
                    formBind: true,
                    hidden: !LABKEY.Security.currentUser.isAdmin,
                    handler: function(btn){
                        var form = btn.up('form');
                        var obj = {};

                        form.getForm().getFields().each(function(f){
                        if(f.dataIndex == 'active'){
                            obj.notifications = obj.notifications || {};
                            obj.notifications[f.notification] = f.getValue() == 'Enabled' ? true : false;
                        }
                        else if (f.dataIndex == 'serviceEnabled'){
                            obj[f.dataIndex] = f.getValue() == 'Enabled' ? true : false;
                        }
                        else {
                            obj[f.dataIndex] = f.getValue();
                        }
                    }, this);

                    if (!LABKEY.Utils.isEmptyObj(obj)){
                        if(obj.notifications){
                            obj.notifications = Ext4.JSON.encode(obj.notifications);
                        }
                        Ext4.Ajax.request({
                            url: LABKEY.ActionURL.buildURL('ldk', 'setNotificationSettings'),
                            params: obj,
                            scope: this,
                            success: function(response){
                                form.getForm().getFields().each(function(f){
                                    f.resetOriginalValue();
                                }, this);

                                Ext4.Msg.alert('Success', 'Save Complete', function(){
                                    window.location.reload();
                                });
                            },
                            failure: LABKEY.Utils.displayAjaxErrorResponse
                        })
                    }
                    else
                        alert('No changes, nothing to save');
                }
                },{
                    text: 'Cancel',
                    handler: function(btn){
                        window.location = LABKEY.ActionURL.buildURL('project', 'start');
                    }
                }]
            }]
        });
    }
});

Ext4.define('LDK.window.ManageNotificationWindow', {
    extend: 'Ext.window.Window',

    initComponent: function(){
        Ext4.apply(this, {
            modal: true,
            title: 'Manage Subscribed Users',
            width: 480,
            closeAction: 'destroy',
            items: [{
                itemId: 'thePanel',
                border: true,
                defaults: {
                    border: false
                },
                bodyStyle: 'padding: 5px;',
                items: [{
                    html: 'Loading...'
                }]
            }],
            buttons: [{
                text: 'Close',
                handler: function(btn){
                    btn.up('window').close();
                }
            }]
        });

        this.callParent();

        this.doLoad();
    },

    doLoad: function(){
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('ldk', 'getNotificationSubscriptions', null, {key: this.notification.key}),
            scope: this,
            failure: LDK.Utils.getErrorCallback(),
            success: LABKEY.Utils.getCallbackWrapper(this.onLoad, this)
        });
    },

    onLoad: function(response){
        var toAdd = [{
            xtype: 'labkey-principalcombo',
            fieldLabel: 'Add User Or Group',
            labelWidth: 150,
            width: 430,
            cache: Ext4.create('Security.util.SecurityCache', {
                root: '/'
            }),
            listeners: {
                scope: this,
                select: function(field, recs){
                    if (!recs || !recs.length)
                        return;

                    field.setValue(null);

                    var userId = recs[0].get('UserId');
                    Ext4.Msg.wait('Updating...');
                    LABKEY.Ajax.request({
                        url: LABKEY.ActionURL.buildURL('ldk', 'updateNotificationSubscriptions'),
                        params: {
                            toAdd: [userId],
                            key: this.notification.key
                        },
                        scope: this,
                        failure: LDK.Utils.getErrorCallback(),
                        success: LABKEY.Utils.getCallbackWrapper(function(response){
                            Ext4.Msg.hide();
                            this.doLoad();
                        }, this)
                    });
                }
            }
        }];

        if (!response.subscriptions || !response.subscriptions.length){
            toAdd.push({
                html: 'There are no subscriptions to this notification',
                style: 'padding-top: 10px;'
            });
        }
        else {
            response.subscriptions = LDK.Utils.sortByProperty(response.subscriptions, 'name');

            var subscribedItems = [];
            Ext4.each(response.subscriptions, function(item){
                subscribedItems.push({
                    layout: 'hbox',
                    bodyStyle: 'padding-bottom: 5px;',
                    items: [{
                        html: item.name,
                        border: false,
                        width: 200
                    },{
                        xtype: 'button',
                        height: 20,
                        border: true,
                        text: 'Remove',
                        userPrincipal: item,
                        scope: this,
                        handler: function(btn){
                            Ext4.Msg.wait('Updating...');
                            LABKEY.Ajax.request({
                                url: LABKEY.ActionURL.buildURL('ldk', 'updateNotificationSubscriptions'),
                                params: {
                                    toRemove: [btn.userPrincipal.userId],
                                    key: this.notification.key
                                },
                                scope: this,
                                failure: LDK.Utils.getErrorCallback(),
                                success: LABKEY.Utils.getCallbackWrapper(function(response){
                                    Ext4.Msg.hide();
                                    this.doLoad();
                                }, this)
                            });
                        }
                    }]
                });
            }, this);

            toAdd.push({
                layout: 'hbox',
                style: 'padding-top: 10px',
                border: false,
                defaults: {
                    border: false
                },
                items: [{
                    width: 160,
                    html: 'Subscribed Users:'
                },{
                    items: subscribedItems,
                    border: false,
                    defaults: {
                        border: false
                    }
                }]
            });
        }

        var target = this.down('#thePanel');
        if (target){
            target.removeAll();
            target.add(toAdd);
        }
    }
});
