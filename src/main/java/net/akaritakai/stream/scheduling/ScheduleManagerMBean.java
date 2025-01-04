package net.akaritakai.stream.scheduling;

import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import javax.management.NotificationEmitter;
import javax.management.ObjectName;
import java.util.Set;

public interface ScheduleManagerMBean extends NotificationEmitter {
    SchedulerAttribute<ObjectName> KEY = SchedulerAttribute.instanceOf(ScheduleManagerMBean.class.getName(), ObjectName.class);

    void triggerIfExists(String message, String chat, JobDataMap jobDataMap);
    void triggerIfExists(String message, String chat);
    void triggerIfExists(String message);

    SchedulerMetaData getMetaData() throws SchedulerException;

    boolean isInStandbyMode() throws SchedulerException;

    void standby() throws SchedulerException;

    void pauseAll() throws SchedulerException;

    void resumeAll() throws SchedulerException;

    Set<JobKey> getJobKeys(GroupMatcher<JobKey> jobKeyGroupMatcher) throws SchedulerException;

    JobDetail getJobDetail(JobKey jobKey) throws SchedulerException;

    Set<? extends TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> triggerKeyGroupMatcher) throws SchedulerException;

    Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException;
}
