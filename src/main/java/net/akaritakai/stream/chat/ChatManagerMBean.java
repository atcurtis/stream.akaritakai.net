package net.akaritakai.stream.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.akaritakai.stream.models.chat.response.ChatStatusResponse;
import net.akaritakai.stream.scheduling.SchedulerAttribute;

import javax.management.NotificationEmitter;
import javax.management.ObjectName;
import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public interface ChatManagerMBean extends NotificationEmitter {
    SchedulerAttribute<ObjectName> KEY = SchedulerAttribute.instanceOf(ChatManagerMBean.class.getName(), ObjectName.class) ;

    void sendMessage(String chatSendRequest);
    void disableChat(String chatDisableRequest);
    void enableChat(String chatEnableRequest);
    public void clearChat(String chatClearRequest);
    String joinChat(String chatJoinRequest) throws JsonProcessingException;
    List<String> listEmojis(String regexp, int limit);
    void setCustomEmoji(String token, String url);
    String getCustomEmoji(String token);
    long getPosition();
    Instant getEpoch();
    boolean isEnabled();
}
