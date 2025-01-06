package net.akaritakai.stream.chat;

import net.akaritakai.stream.models.chat.request.ChatJoinRequest;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;
import net.akaritakai.stream.models.chat.response.ChatStatusResponse;
import net.akaritakai.stream.scheduling.SchedulerAttribute;

import javax.annotation.Nonnull;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;
import java.time.Instant;
import java.util.List;

public interface ChatManagerMBean extends NotificationEmitter {
    SchedulerAttribute<ObjectName> KEY = SchedulerAttribute.instanceOf(ChatManagerMBean.class.getName(), ObjectName.class) ;

    void sendMessage(@Nonnull ChatSendRequest request);
    void sendCommand(@Nonnull ChatSendRequest request);
    void disableChat();
    void enableChat();
    void clearChat();
    ChatStatusResponse joinChat(@Nonnull ChatJoinRequest request);
    List<String> listEmojis(String regexp, int limit);
    void setCustomEmoji(String token, String url);
    String getCustomEmoji(String token);
    long getPosition();
    Instant getEpoch();
    boolean isEnabled();
}
