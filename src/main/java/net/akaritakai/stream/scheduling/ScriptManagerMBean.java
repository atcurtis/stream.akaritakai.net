package net.akaritakai.stream.scheduling;

import net.akaritakai.stream.models.quartz.TaskEntry;
import net.akaritakai.stream.models.quartz.TaskResult;

import javax.management.ObjectName;

public interface ScriptManagerMBean {
    SchedulerAttribute<ObjectName> KEY = SchedulerAttribute.instanceOf(ScriptManagerMBean.class.getName(), ObjectName.class);

    TaskResult executeTask(TaskEntry task);
}
