package net.akaritakai.stream.script;

import net.akaritakai.stream.models.quartz.TaskEntry;
import net.akaritakai.stream.models.quartz.TaskResult;
import net.akaritakai.stream.scheduling.SchedulerAttribute;

import javax.management.ObjectName;

public interface ScriptManagerMBean {
    SchedulerAttribute<ObjectName> KEY = SchedulerAttribute.instanceOf(ScriptManagerMBean.class.getName(), ObjectName.class);

    TaskResult executeTask(TaskEntry task);
}
