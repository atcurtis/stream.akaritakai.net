package net.akaritakai.stream.chat;

import net.akaritakai.stream.exception.ChatStateConflictException;
import net.akaritakai.stream.models.chat.commands.ChatClearRequest;
import net.akaritakai.stream.models.chat.commands.ChatDisableRequest;
import net.akaritakai.stream.models.chat.commands.ChatEnableRequest;
import net.akaritakai.stream.models.chat.request.ChatJoinRequest;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;
import net.akaritakai.stream.models.chat.response.ChatStatusResponse;

import javax.management.NotificationEmitter;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public interface ChatManagerMXBean extends NotificationEmitter {
    void sendMessage(ChatSendRequest request, InetAddress source);
    void disableChat(ChatDisableRequest request);
    void enableChat(ChatEnableRequest request);
    public void clearChat(ChatClearRequest request);
    ChatStatusResponse joinChat(ChatJoinRequest request);
    List<Map.Entry<String, String>> listEmojis(Predicate<String> matcher, int limit);
    void setCustomEmoji(String key, String url);
}
