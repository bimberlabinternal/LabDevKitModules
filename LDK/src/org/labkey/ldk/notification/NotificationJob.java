package org.labkey.ldk.notification;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
    private static final Logger _log = LogManager.getLogger(NotificationJob.class);
    private Notification _notification;

    @Override
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
            NotificationServiceImpl.get().runForContainer(_notification, c);
        }

        NotificationServiceImpl.get().setLastRun(_notification, new Date().getTime());
    }
}