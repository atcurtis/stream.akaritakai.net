package net.akaritakai.stream.scheduling;

import net.akaritakai.stream.models.quartz.JobEntry;
import net.akaritakai.stream.models.quartz.KeyEntry;
import net.akaritakai.stream.models.quartz.TriggerEntry;
import net.akaritakai.stream.models.quartz.response.StatusResponse;
import org.quartz.*;

import javax.management.NotificationEmitter;
import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

public interface ScheduleManagerMBean extends NotificationEmitter {
    SchedulerAttribute<ObjectName> KEY = SchedulerAttribute.instanceOf(ScheduleManagerMBean.class.getName(), ObjectName.class);

    void triggerIfExists(String message, String chat, Map<String, String> jobDataMap);
    void triggerIfExists(String message, String chat);
    void triggerIfExists(String message);

    StatusResponse getMetaData() throws SchedulerException;

    boolean isInStandbyMode() throws SchedulerException;

    void standby() throws SchedulerException;

    void pauseAll() throws SchedulerException;

    void resumeAll() throws SchedulerException;

    Set<KeyEntry> getJobKeys(String groupPrefix) throws SchedulerException;

    JobEntry getJobDetail(KeyEntry jobKey) throws SchedulerException;

    Set<KeyEntry> getTriggerKeys(String groupPrefix) throws SchedulerException;

    TriggerEntry getTrigger(KeyEntry triggerKey) throws SchedulerException;
}
