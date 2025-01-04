package net.akaritakai.stream.scheduling.jobs;

import net.akaritakai.stream.chat.ChatManagerMBean;
import org.quartz.JobExecutionContext;

public class ChatEnableJob extends AbstractChatJob {
    @Override
    protected void execute1(JobExecutionContext context, ChatManagerMBean chatManager) throws Exception {
        chatManager.enableChat();
    }
}
