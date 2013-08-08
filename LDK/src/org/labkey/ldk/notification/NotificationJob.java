package org.labkey.ldk.notification;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.ldk.notification.Notification;
import org.labkey.api.ldk.notification.NotificationService;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.MailHelper;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import javax.mail.Address;
import javax.mail.Message;
import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 12/19/12
 * Time: 7:59 PM
 */
public class NotificationJob implements Job
{
    private static final Logger _log = Logger.getLogger(NotificationJob.class);
    private Notification _notification;

    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        String key = context.getMergedJobDataMap().getString("notificationName");
        _notification = NotificationServiceImpl.get().getNotification(key);
        if (_notification == null)
        {
            throw new JobExecutionException("Unknown notification: " + key);
        }

        if (!NotificationService.get().isServiceEnabled())
        {
            //_log.info("Notification service has been disabled at the site level, will not run notification: " + _notification.getName());
            return;
        }

        _log.info("Trying to run notification: " + _notification.getName());

        Set<Container> activeContainers = NotificationServiceImpl.get().getActiveContainers(_notification);
        if (activeContainers.size() == 0)
        {
            _log.info("there are no active containers, skipping");
        }

        for (Container c : activeContainers)
        {
            runForContainer(c);
        }

        NotificationServiceImpl.get().setLastRun(_notification, new Date().getTime());
    }

    private void runForContainer(Container c)
    {
        User u = NotificationService.get().getUser(_notification, c);
        if (u == null)
        {
            _log.error("Invalid user when trying to run notification " + _notification.getName() + " for container: " + c.getPath());
            return;
        }

        if (!c.hasPermission(u, AdminPermission.class))
        {
            _log.error("Error running " + _notification.getName() + ".  User " + u.getEmail() + " does not have admin permissions on container: " + c.getPath());
            return;
        }

        if (!NotificationServiceImpl.get().isActive(_notification, c))
        {
            _log.error("Error running " + _notification.getName() + " in container : " + c.getPath() + ".  Notification is inactive, but a task was still scheduled");
            return;
        }

        List<Address> recipients = NotificationServiceImpl.get().getEmails(_notification, c);
        if (recipients.size() == 0)
        {
            _log.info("Notification: " + _notification.getName() + " has no recipients, skipping");
            return;
        }

        try
        {
            String msg = _notification.getMessage(c, u);
            if (StringUtils.isEmpty(msg))
            {
                _log.info("Notification " + _notification.getName() + " did not produce a message, will not send email");
                return;
            }

            _log.info("Sending message for notification: " + _notification.getName() + " to " + recipients.size() + " recipients");
            MailHelper.MultipartMessage mail = MailHelper.createMultipartMessage();

            mail.setFrom(NotificationServiceImpl.get().getReturnEmail(c));
            mail.setSubject(_notification.getEmailSubject());
            mail.setBodyContent(msg, "text/html");
            mail.addRecipients(Message.RecipientType.TO, recipients.toArray(new Address[recipients.size()]));

            MailHelper.send(mail, u, c);
        }
        catch (Exception e)
        {
            ExceptionUtil.logExceptionToMothership(null, e);
        }
    }
}