package net.akaritakai.stream.chat;

import java.net.InetAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.akaritakai.stream.exception.ChatStateConflictException;
import net.akaritakai.stream.handler.Util;
import net.akaritakai.stream.models.chat.ChatMessage;
import net.akaritakai.stream.models.chat.ChatMessageType;
import net.akaritakai.stream.models.chat.ChatSequence;
import net.akaritakai.stream.models.chat.commands.ChatClearRequest;
import net.akaritakai.stream.models.chat.commands.ChatDisableRequest;
import net.akaritakai.stream.models.chat.commands.ChatEnableRequest;
import net.akaritakai.stream.models.chat.request.ChatJoinRequest;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;
import net.akaritakai.stream.models.chat.response.ChatStatusResponse;
import net.akaritakai.stream.scheduling.SchedulerAttribute;
import net.akaritakai.stream.scheduling.Utils;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;


public class ChatManager extends NotificationBroadcasterSupport implements ChatManagerMXBean {
  public static final SchedulerAttribute<ChatManager> KEY = SchedulerAttribute.instanceOf("chatManager", ChatManager.class);
  private static final Logger LOG = LoggerFactory.getLogger(ChatManager.class);

  private final AtomicReference<ChatHistory> _history = new AtomicReference<>(null);

  private final ConcurrentMap<String, String> _customEmojiMap = new ConcurrentHashMap<>();
  private final Scheduler _scheduler;
  private final AtomicInteger _sequenceNumber = new AtomicInteger();

  public ChatManager(Scheduler scheduler) {
    _scheduler = scheduler;
  }

  @Override
  public void sendMessage(ChatSendRequest request, InetAddress source) throws ChatStateConflictException {
    LOG.debug("Got ChatSendRequest = {}", request);
    ChatHistory currentHistory = _history.get();
    if (currentHistory == null) {
      throw new ChatStateConflictException("Chat is disabled");
    } else {
      if (request.getMessageType() == ChatMessageType.EMOJI) {
        currentHistory.setActiveEmoji(request.getNickname(), request.getMessage());
      } else if (request.getMessageType() == ChatMessageType.TEXT) {
        StringTokenizer st = new StringTokenizer(request.getMessage());
        while (st.hasMoreTokens()) {
          String token = st.nextToken();
          if (!token.startsWith(":") || !token.endsWith(":")) {
            continue;
          }
          if (!_customEmojiMap.containsKey(token)) {
            int sep = token.indexOf("::");
            if (sep < 1) {
              continue;
            }
            token = token.substring(0, sep + 1);
            if (!_customEmojiMap.containsKey(token)) {
              continue;
            }
          }
          String emoji = _customEmojiMap.get(token);
          if (!currentHistory.isActiveEmoji(token, emoji)) {
            sendMessage(ChatSendRequest.builder()
                    .messageType(ChatMessageType.EMOJI)
                    .nickname(token)
                    .message(String.valueOf(emoji))
                    .build(), Util.ANY);
          }
        }
      }
      ChatMessage message = currentHistory.addMessage(request, source);
      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
              "sendMessage", "Message", ChatMessage.class.getName(), null, message);
      sendNotification(n);
      JobDataMap jobDataMap = new JobDataMap();
      jobDataMap.put("type", String.valueOf(request.getMessageType()));
      jobDataMap.put("nick", request.getNickname());
      jobDataMap.put("message", request.getMessage());
      Utils.triggerIfExists(_scheduler, "Message", "Chat", jobDataMap);
    }
  }

  @Override
  public void disableChat(ChatDisableRequest request) {
    LOG.info("Got ChatDisableRequest");
    assert request != null;
    ChatHistory currentHistory = _history.get();
    if (currentHistory == null) {
      throw new ChatStateConflictException("Chat is already disabled");
    } else {
      ChatHistory oldHistory = _history.getAndSet(null);
      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
              "disableChat", "History", ChatHistory.class.getName(), oldHistory, null);
      sendNotification(n);
      Utils.triggerIfExists(_scheduler, "disableChat", "Chat");
    }
  }

  @Override
  public void enableChat(ChatEnableRequest request) {
    LOG.info("Got ChatEnableRequest");
    assert request != null;
    ChatHistory currentHistory = _history.get();
    if (currentHistory != null) {
      throw new ChatStateConflictException("Chat is already enabled");
    } else {
      ChatHistory newHistory = new ChatHistory();
      ChatHistory oldHistory = _history.getAndSet(newHistory);
      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
              "enableChat", "History", ChatHistory.class.getName(), oldHistory, newHistory);
      sendNotification(n);
      Utils.triggerIfExists(_scheduler, "enableChat", "Chat");
    }
  }

  @Override
  public void clearChat(ChatClearRequest request) {
    LOG.info("Got ChatClearRequest");
    assert request != null;
    ChatHistory currentHistory = _history.get();
    if (currentHistory == null) {
      throw new ChatStateConflictException("Chat is disabled");
    } else {
      ChatHistory newHistory = new ChatHistory();
      ChatHistory oldHistory = _history.getAndSet(newHistory);
      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
              "clearChat", "History", ChatHistory.class.getName(), oldHistory, newHistory);
      sendNotification(n);
      Utils.triggerIfExists(_scheduler, "clearChat", "Chat");
    }
  }


  @Override
  public ChatStatusResponse joinChat(ChatJoinRequest request) {
    LOG.info("Got ChatJoinRequest = {}", request);
    ChatHistory history = _history.get();
    if (history == null) {
      return ChatStatusResponse.builder()
          .enabled(false)
          .build();
    } else {
      Instant requestEpoch = Optional.ofNullable(request)
          .map(ChatJoinRequest::getSequence)
          .map(ChatSequence::getEpoch)
          .orElse(Instant.EPOCH);
      long requestPosition = Optional.ofNullable(request)
          .map(ChatJoinRequest::getSequence)
          .map(ChatSequence::getPosition)
          .orElse(0L);

      List<ChatMessage> messages = history.fetchMessages(requestEpoch, requestPosition);
      long position = 0;
      if (!messages.isEmpty()) {
        ChatMessage lastMessage = messages.get(messages.size() - 1);
        position = lastMessage.getSequence().getPosition();
      }

      return ChatStatusResponse.builder()
          .enabled(true)
          .sequence(ChatSequence.builder()
              .epoch(history.getEpoch())
              .position(position)
              .build())
          .messages(messages)
          .build();
    }
  }

  @Override
  public List<Map.Entry<String, String>> listEmojis(Predicate<String> matcher, int limit) {
      return _customEmojiMap.entrySet().stream()
              .filter(entry -> matcher.test(entry.getKey()))
              .limit(limit)
              .sorted(Map.Entry.comparingByKey())
              .collect(Collectors.toList());
  }

  @Override
  public void setCustomEmoji(String key, String url) {
    if (key.startsWith(":") && key.endsWith(":")) {
      _customEmojiMap.put(key, url);
    } else {
      throw new IllegalArgumentException("emoji starts and ends with a colon");
    }
  }
}
