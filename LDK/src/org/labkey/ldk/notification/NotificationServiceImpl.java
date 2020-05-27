/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.ldk.notification;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.ldk.notification.Notification;
import org.labkey.api.ldk.notification.NotificationService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.Group;
import org.labkey.api.security.MemberType;
import org.labkey.api.security.SecurityManager;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.ValidEmail;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.util.ConfigurationException;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.MailHelper;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.ldk.LDKSchema;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 7/14/12
 * Time: 2:49 PM
 */
public class NotificationServiceImpl extends NotificationService
{
    private final static Logger _log = Logger.getLogger(NotificationServiceImpl.class);

    public final static String TIMESTAMP_PROPERTY_DOMAIN = "org.labkey.ldk.notifications.timestamp";
    public final static String STATUS_PROPERTY_DOMAIN = "org.labkey.ldk.notifications.status";
    public final static String CONFIG_PROPERTY_DOMAIN = "org.labkey.ldk.notifications.config";

    private static final String ENABLED_PROP = "serviceEnabled";
    private static final String RETURN_EMAIL = "returnEmail";
    private static final String USER_PROP = "user";

    private List<Notification> _notifications = new ArrayList<>();
    private Map<Notification, Trigger> _triggerMap = new HashMap<>();

    public static NotificationServiceImpl get()
    {
        return (NotificationServiceImpl) NotificationService.get();
    }

    public NotificationServiceImpl()
    {

    }

    private Set<Container> getEnabledContainers()
    {
        Set<Container> containers = new HashSet<>();
        PropertyManager.PropertyEntry[] entries = PropertyManager.findPropertyEntries(null, null, STATUS_PROPERTY_DOMAIN, null);
        for (PropertyManager.PropertyEntry e : entries)
        {
            if (e.getObjectId() != null && Boolean.parseBoolean(e.getValue()))
            {
                Container c = ContainerManager.getForId(e.getObjectId());
                if (c != null)
                    containers.add(c);
            }
        }
        return containers;
    }

    @Override
    public void registerNotification(Notification notification)
    {
        _log.info("Registering notification: " + notification.getName());
        _notifications.add(notification);

        try
        {
            schedule(notification);
        }
        catch (SchedulerException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void schedule(Notification n) throws SchedulerException
    {
        if (n.getCronString() == null)
            return; //allows for alerts that are only used on-demand

        JobDetail job = JobBuilder.newJob(NotificationJob.class)
                .withIdentity(n.getClass().getCanonicalName(), getClass().getCanonicalName())
                .usingJobData("notificationName", NotificationServiceImpl.get().getKey(n))
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(n.getClass().getCanonicalName(), getClass().getCanonicalName())
                .withSchedule(CronScheduleBuilder.cronSchedule(n.getCronString()))
                .forJob(job)
                .build();

        StdSchedulerFactory.getDefaultScheduler().scheduleJob(job, trigger);
        _triggerMap.put(n, trigger);
    }

    private static String getConfigProperty(Container c, String key)
    {
        return PropertyManager.getProperties(c, CONFIG_PROPERTY_DOMAIN).get(key);
    }

    @Override
    public boolean isServiceEnabled()
    {
        String prop = getConfigProperty(ContainerManager.getRoot(), ENABLED_PROP);
        if (prop == null)
            return false;

        return Boolean.parseBoolean(prop);
    }

    public void setServiceEnabled(Boolean status)
    {
        PropertyManager.PropertyMap pm = PropertyManager.getWritableProperties(ContainerManager.getRoot(), NotificationServiceImpl.CONFIG_PROPERTY_DOMAIN, true);
        pm.put(ENABLED_PROP, status.toString());
        pm.save();
    }

    @Override
    public Set<Notification> getNotifications(Container c, boolean includeAll)
    {
        Set<Notification> notifications = new HashSet<>();
        if (c.equals(ContainerManager.getRoot()))
        {
            notifications.addAll(_notifications);
        }
        else
        {
            for (Notification n : _notifications)
            {
                if (includeAll)
                {
                    notifications.add(n);
                }
                else if (n.isAvailable(c))
                {
                    notifications.add(n);
                }
            }
        }

        return Collections.unmodifiableSet(notifications);
    }

    @Override
    public Notification getNotification(String key)
    {
        for (Notification n : _notifications)
        {
            if (getKey(n).equals(key))
                return n;
        }
        return null;
    }

    @Override
    public Address getReturnEmail(Container c)
    {
        String email = getConfigProperty(c, RETURN_EMAIL);
        return email == null ? null : getAddress(email);
    }

    public void setReturnEmail(Container c, String returnEmail)
    {
        try
        {
            if(returnEmail != null)
            {
                ValidEmail email = new ValidEmail(returnEmail);

                PropertyManager.PropertyMap pm = PropertyManager.getWritableProperties(c, NotificationServiceImpl.CONFIG_PROPERTY_DOMAIN, true);
                pm.put(RETURN_EMAIL, email.getEmailAddress());
                pm.save();
            }
        }
        catch (ValidEmail.InvalidEmailException e)
        {
            throw new ConfigurationException(e.getMessage());
        }
    }

    public User getUser(Container c)
    {
        String user = getConfigProperty(c, USER_PROP);
        if (StringUtils.isEmpty(user))
            return null;

        return UserManager.getUser(Integer.parseInt(user));
    }

    public void setUser(Container c, Integer userId)
    {
        if(userId != null)
        {
            PropertyManager.PropertyMap pm = PropertyManager.getWritableProperties(c, NotificationServiceImpl.CONFIG_PROPERTY_DOMAIN, true);
            pm.put(USER_PROP, String.valueOf(userId));
            pm.save();
        }
    }

    private Address getAddress(String email)
    {
        try
        {
            ValidEmail ve = new ValidEmail(email);
            return ve.getAddress();
        }
        catch (ValidEmail.InvalidEmailException e)
        {
            throw new ConfigurationException(e.getMessage());
        }
    }

    public JSONObject getJson(Notification n, Container c, User u)
    {
        JSONObject json = new JSONObject();
        json.put("name", n.getName());
        json.put("description", n.getDescription());
        json.put("active", isActive(n, c));
        json.put("available", n.isAvailable(c));
        json.put("schedule", n.getScheduleDescription());
        json.put("cronString", n.getCronString());
        long lastRun = getLastRun(n);
        json.put("lastRun", lastRun);
        json.put("lastRunDate", lastRun == 0 ? null : new Date(lastRun));
        String duration = lastRun == 0 ? "" : DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - lastRun, true, true);
        json.put("durationString", duration);
        json.put("key", getKey(n));
        json.put("category", n.getCategory());
        json.put("nextFireTime", getNextFireTime(n));

        //find subscriptions for the current user
        List<UserPrincipal> ups = new ArrayList<>();
        for (UserPrincipal up : NotificationServiceImpl.get().getRecipients(n, c))
        {
            if (up instanceof Group)
            {
                Group g = (Group)up;
                Set<UserPrincipal> members = SecurityManager.getAllGroupMembers(g, MemberType.ALL_GROUPS_AND_USERS);
                if (members.contains(u))
                    ups.add(up);
            }
            else
            {
                if (up.getUserId() == u.getUserId())
                    ups.add(up);
            }
        }

        List<Map<String, Object>> subscriptions = new ArrayList<>();
        for (UserPrincipal up : ups)
        {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", up.getUserId());
            map.put("type", up.getType());
            map.put("name", up.getName());
            subscriptions.add(map);
        }
        json.put("subscriptions", subscriptions);

        return json;
    }

    public String getKey(Notification n)
    {
        return n.getClass().getCanonicalName();
    }

    @Override
    public long getLastRun(Notification n)
    {
        Map<String, String> m = PropertyManager.getProperties(NotificationServiceImpl.TIMESTAMP_PROPERTY_DOMAIN);
        String value = m.get(getKey(n));
        if (value != null)
        {
            return Long.parseLong(value);
        }
        return 0;

    }

    public Date getLastRunDate(Notification n)
    {
        long lastRun = getLastRun(n);
        return lastRun == 0 ? null : new Date(lastRun);
    }

    public void setLastRun(Notification n, Long lastRun)
    {
        PropertyManager.PropertyMap pm = PropertyManager.getWritableProperties(NotificationServiceImpl.TIMESTAMP_PROPERTY_DOMAIN, true);
        pm.put(getKey(n), lastRun.toString());
        pm.save();
    }

    @Override
    public boolean isActive(Notification n, Container c)
    {
        Map<String, String> m = PropertyManager.getProperties(c, NotificationServiceImpl.STATUS_PROPERTY_DOMAIN);
        String value = m.get(getKey(n));
        if (value != null)
        {
            return Boolean.parseBoolean(value);
        }
        return false;
    }

    public void setActive(Notification n, Container c, boolean active)
    {
        PropertyManager.PropertyMap pm = PropertyManager.getWritableProperties(c, NotificationServiceImpl.STATUS_PROPERTY_DOMAIN, true);
        pm.put(getKey(n), active ? String.valueOf(active) : null);
        pm.save();
    }

    @Override
    @NotNull
    public List<Address> getEmailsForPrincipal(UserPrincipal user) throws ValidEmail.InvalidEmailException
    {
        if (user instanceof User)
        {
            if (!((User) user).isActive())
            {
                //_log.error("an inactive user is a notification recipient: " + user.getName());
                return Collections.emptyList();
            }

            ValidEmail validEmail = new ValidEmail(((User)user).getEmail());
            return Collections.singletonList(validEmail.getAddress());
        }
        else
        {
            Group group = ((Group)user);
            if (group != null)
            {
                if (group.isSystemGroup())
                    throw new IllegalArgumentException("Invalid group ID: site groups are not allowed");

                //NOTE: this could include inactive users as members, so we filter them out
                Set<User> members = SecurityManager.getAllGroupMembers(group, MemberType.ACTIVE_USERS);
                List<Address> addresses = new ArrayList<>();
                for (User u : members)
                {
                    //note: unlike user, dont log error since inactive users retain group membership
                    if (!u.isActive())
                    {
                        continue;
                    }

                    ValidEmail validEmail = new ValidEmail(u.getEmail());
                    addresses.add(validEmail.getAddress());
                }

                return addresses;
            }
            else
                throw new IllegalArgumentException("Unable to resolve principalId");
        }
    }

    public String getMessage(Notification n, Container c) throws IOException, MessagingException
    {
        if (!n.isAvailable(c))
            return null;

        User u = getUser(c);
        if (u == null)
            return null;

        MimeMessage message = n.createMessage(c, u);
        if (message != null)
        {
            Map<String, String> map = MailHelper.getBodyParts(message);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : map.entrySet())
            {
                if ("text/html".equals(entry.getKey()))
                {
                    sb.append(entry.getValue());
                }
                else if (entry.getKey().startsWith("text/"))
                {
                    sb.append(PageFlowUtil.filter(entry.getValue()));
                }
                else
                {
                    sb.append("Attached file. MIME ContentType: " + entry.getKey() + " (not rendered here)");
                }
                sb.append("<br/><br/><br/>");
            }
            return sb.toString();
        }

        return null;
    }

    @Override
    public User getUser(Notification n, Container c)
    {
        return getUser(c);
    }

    @Override
    public Set<UserPrincipal> getRecipients(Notification n, Container c)
    {
        final Set<UserPrincipal> recipients = new HashSet<>();
        TableInfo t = LDKSchema.getInstance().getTable(LDKSchema.TABLE_NOTIFICATION_RECIPIENTS);
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("notificationtype"), n.getName(), CompareType.EQUAL);
        filter.addCondition(FieldKey.fromString("container"), c.getId(), CompareType.EQUAL);
        TableSelector ts = new TableSelector(t, Collections.singleton("recipient"), filter, null);
        if (ts.getRowCount() > 0)
        {
            ts.forEach(new TableSelector.ForEachBlock<ResultSet>(){
                @Override
                public void exec(ResultSet rs) throws SQLException
                {
                    int userId = rs.getInt("recipient");
                    UserPrincipal u = SecurityManager.getPrincipal(userId);
                    if (u != null)
                        recipients.add(u);
                    else
                        _log.error("unknown user registered with " + n.getName() + "notification: " + userId);
                }
            });
        }

        return recipients;
    }

    public List<Address> getEmails(Notification n, Container c)
    {
        List<Address> addresses = new ArrayList<>();
        for (UserPrincipal u : getRecipients(n, c))
        {
            try
            {
                addresses.addAll(getEmailsForPrincipal(u));
            }
            catch (ValidEmail.InvalidEmailException e)
            {
                _log.error(e);
            }
        }

        return addresses;
    }

    public Set<Container> getActiveContainers(Notification n)
    {
        Set<Container> containers = new HashSet<>();
        for (Container c : getEnabledContainers())
        {
            if (isActive(n, c))
            {
                containers.add(c);
            }
        }

        return containers;
    }

    @Override
    public Date getNextFireTime(Notification n)
    {
        Trigger trigger = _triggerMap.get(n);
        if (trigger == null)
            return null;

        return trigger.getFireTimeAfter(new Date());
    }

    public void updateSubscriptions(Container c, User u, Notification n, @Nullable List<UserPrincipal> toAdd, @Nullable List<UserPrincipal> toRemove)
    {
        TableInfo ti = LDKSchema.getTable(LDKSchema.TABLE_NOTIFICATION_RECIPIENTS);

        if (toAdd != null)
        {
            for (UserPrincipal up : toAdd)
            {
                SimpleFilter filter = new SimpleFilter(FieldKey.fromString("container"), c.getId(), CompareType.EQUAL);
                filter.addCondition(FieldKey.fromString("recipient"), up.getUserId());
                filter.addCondition(FieldKey.fromString("notificationtype"), n.getName());

                TableSelector ts = new TableSelector(ti, filter, null);
                if (ts.getRowCount() == 0)
                {
                    Map<String, Object> row = new HashMap<>();
                    row.put("container", c.getId());
                    row.put("recipient", up.getUserId());
                    row.put("notificationtype", n.getName());
                    row.put("createdby", u.getUserId());
                    row.put("created", new Date());
                    Table.insert(u, ti, row);
                }
            }

            if (toRemove != null)
            {
                for (UserPrincipal up : toRemove)
                {
                    SimpleFilter filter = new SimpleFilter(FieldKey.fromString("container"), c.getId(), CompareType.EQUAL);
                    filter.addCondition(FieldKey.fromString("recipient"), up.getUserId());
                    filter.addCondition(FieldKey.fromString("notificationtype"), n.getName());

                    Table.delete(ti, filter);
                }
            }
        }
    }

    public void runForContainer(Notification notification, Container c)
    {
        User u = NotificationService.get().getUser(notification, c);
        if (u == null)
        {
            _log.error("Invalid user when trying to run notification " + notification.getName() + " for container: " + c.getPath());
            return;
        }

        if (!c.hasPermission(u, AdminPermission.class))
        {
            _log.error("Error running " + notification.getName() + ".  User " + u.getEmail() + " does not have admin permissions on container: " + c.getPath());
            return;
        }

        if (!NotificationServiceImpl.get().isActive(notification, c))
        {
            _log.error("Error running " + notification.getName() + " in container : " + c.getPath() + ".  Notification is inactive, but a task was still scheduled");
            return;
        }

        List<Address> recipients = NotificationServiceImpl.get().getEmails(notification, c);
        if (recipients.size() == 0)
        {
            _log.info("Notification: " + notification.getName() + " has no recipients, skipping");
            return;
        }

        try
        {
            MimeMessage mail = notification.createMessage(c, u);
            if (mail == null)
            {
                _log.info("Notification " + notification.getName() + " did not produce a message, will not send email");
                return;
            }

            _log.info("Sending message for notification: " + notification.getName() + " to " + recipients.size() + " recipients");

            mail.setFrom(NotificationServiceImpl.get().getReturnEmail(c));
            mail.setSubject(notification.getEmailSubject(c));
            mail.addRecipients(Message.RecipientType.TO, recipients.toArray(new Address[recipients.size()]));

            MailHelper.send(mail, u, c);
        }
        catch (Exception e)
        {
            ExceptionUtil.logExceptionToMothership(null, e);
        }
    }
}
