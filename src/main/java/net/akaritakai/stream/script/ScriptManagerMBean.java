package net.akaritakai.stream.script;

import net.akaritakai.stream.models.quartz.TaskEntry;
import net.akaritakai.stream.models.quartz.TaskResult;
import net.akaritakai.stream.scheduling.SchedulerAttribute;

import javax.management.ObjectName;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public interface ScriptManagerMBean {
    SchedulerAttribute<ObjectName> KEY = SchedulerAttribute.instanceOf(ScriptManagerMBean.class.getName(), ObjectName.class);

    TaskResult executeTask(TaskEntry task, Map<String, Object> map);

    TaskEntry loadTask(String name, String group) throws SQLException;
    boolean saveTask(TaskEntry task) throws SQLException;

    Set<String> getAllTaskGroups() throws SQLException;
    Set<String> getAllTaskNames(String group) throws SQLException;
}
