package net.akaritakai.stream.scheduling.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.akaritakai.stream.models.quartz.TaskEntry;
import net.akaritakai.stream.models.quartz.TaskResult;
import net.akaritakai.stream.scheduling.Utils;
import net.akaritakai.stream.script.ScriptManagerMBean;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import static net.akaritakai.stream.config.GlobalNames.scriptManagerName;

public class ScriptJob extends AbstractJob {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    @Override
    protected void execute0(JobExecutionContext context) throws Exception {
        ScriptManagerMBean scriptManager = Utils.beanProxy(scriptManagerName, ScriptManagerMBean.class);
        JobKey jobKey = context.getJobDetail().getKey();

        TaskEntry task = scriptManager.loadTask(jobKey.getName(), jobKey.getGroup());

        TaskResult result = scriptManager.executeTask(task, context.getMergedJobDataMap());

        if (result.getOutputClassName() != null) {
            Class<?> cls = Class.forName(result.getOutputClassName());
            if (CharSequence.class.isAssignableFrom(cls) || Number.class.isAssignableFrom(cls)) {
                context.setResult(result.getOutput());
            } else {
                context.setResult(OBJECT_MAPPER.readValue(result.getOutput(), cls));
            }
        }
    }
}
