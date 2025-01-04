package net.akaritakai.stream.chat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.config.ConfigData;
import net.akaritakai.stream.debug.TouchTimer;
import net.akaritakai.stream.exception.ChatStateConflictException;
import net.akaritakai.stream.handler.RouterHelper;
import net.akaritakai.stream.handler.Util;
import net.akaritakai.stream.handler.chat.*;
import net.akaritakai.stream.models.chat.ChatMessage;
import net.akaritakai.stream.models.chat.ChatMessageType;
import net.akaritakai.stream.models.chat.ChatSequence;
import net.akaritakai.stream.models.chat.request.ChatJoinRequest;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;
import net.akaritakai.stream.models.chat.response.ChatStatusResponse;
import net.akaritakai.stream.scheduling.ScheduleManagerMBean;
import net.akaritakai.stream.scheduling.Utils;
import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import static net.akaritakai.stream.config.GlobalNames.*;

public class ChatManager extends NotificationBroadcasterSupport implements ChatManagerMBean {
  private static final Logger LOG = LoggerFactory.getLogger(ChatManager.class);

  private final AtomicReference<ChatHistory> _history = new AtomicReference<>(null);

  private final ConcurrentMap<String, String> _customEmojiMap = new ConcurrentHashMap<>();

  private final AtomicInteger _sequenceNumber = new AtomicInteger();

  public ChatManager(TouchTimer startTimer, ConfigData config) {

  }

  public static void register(Vertx vertx, RouterHelper router, CheckAuth checkAuth, ObjectName chatManagerName) {
    router.registerPostApiHandler("/chat/clear", new ChatClearHandler(chatManagerName, checkAuth, vertx));
    router.registerPostApiHandler("/chat/disable", new ChatDisableHandler(chatManagerName, checkAuth, vertx));
    router.registerPostApiHandler("/chat/enable", new ChatEnableHandler(chatManagerName, checkAuth, vertx));
    router.registerPostApiHandler("/chat/write", new ChatWriteHandler(chatManagerName, checkAuth));
    router.registerPostApiHandler("/chat/emojis", new ChatListEmojisHandler(chatManagerName, checkAuth));
    router.registerPostApiHandler("/chat/emoji", new ChatSetEmojiHandler(chatManagerName, checkAuth));
    router.registerGetHandler("/chat", new ChatClientHandler(vertx, chatManagerName));
  }

  public void addCustomEmojis(File jsonFile) throws IOException {
    if (jsonFile != null) {
      for (Map.Entry<String, String> entry : new ObjectMapper()
              .readValue(jsonFile, new TypeReference<Map<String, String>>() {
              }).entrySet()) {
        if (entry.getValue() != null) {
          if (entry.getKey().isBlank()) {
            throw new IllegalArgumentException("key name can't be blank");
          }
          setCustomEmoji(":" + entry.getKey() + ":", new URL(entry.getValue()).toString());
        }
      }
    }
  }

  @Override
  public void sendMessage(@Nonnull ChatSendRequest request) {
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
          String emoji = getCustomEmoji(token);
          if (emoji != null && !currentHistory.isActiveEmoji(token, emoji)) {
            sendMessage(ChatSendRequest.builder()
                    .messageType(ChatMessageType.EMOJI)
                    .nickname(token)
                    .message(emoji)
                    .source(Util.ANY)
                    .build());
          }
        }
      }
      ChatMessage message = currentHistory.addMessage(request);
      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
              "sendMessage", "Message", ChatMessage.class.getName(), null, message);
      sendNotification(n);
      JobDataMap jobDataMap = new JobDataMap();
      jobDataMap.put("type", String.valueOf(request.getMessageType()));
      jobDataMap.put("nick", request.getNickname());
      jobDataMap.put("message", request.getMessage());
      jobDataMap.put("source", request.getSource());
      Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class)
              .triggerIfExists("Message", "Chat", jobDataMap);
    }
  }

  @Override
  public void disableChat() {
    LOG.info("Got ChatDisableRequest");
    ChatHistory currentHistory = _history.get();
    if (currentHistory == null) {
      throw new ChatStateConflictException("Chat is already disabled");
    } else {
      ChatHistory oldHistory = _history.getAndSet(null);
      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
              "disableChat", "History", ChatHistory.class.getName(), oldHistory, null);
      sendNotification(n);
      Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class)
                      .triggerIfExists("disableChat", "Chat");
    }
  }

  @Override
  public void enableChat() {
    LOG.info("Got ChatEnableRequest");
    ChatHistory currentHistory = _history.get();
    if (currentHistory != null) {
      throw new ChatStateConflictException("Chat is already enabled");
    } else {
      ChatHistory newHistory = new ChatHistory();
      ChatHistory oldHistory = _history.getAndSet(newHistory);
      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
              "enableChat", "History", ChatHistory.class.getName(), oldHistory, newHistory);
      sendNotification(n);
      Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class)
              .triggerIfExists("enableChat", "Chat");
    }
  }

  @Override
  public void clearChat() {
    LOG.info("Got ChatClearRequest");
    ChatHistory currentHistory = _history.get();
    if (currentHistory == null) {
      throw new ChatStateConflictException("Chat is disabled");
    } else {
      ChatHistory newHistory = new ChatHistory();
      ChatHistory oldHistory = _history.getAndSet(newHistory);
      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
              "clearChat", "History", ChatHistory.class.getName(), oldHistory, newHistory);
      sendNotification(n);
      Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class)
              .triggerIfExists("clearChat", "Chat");
    }
  }

  @Override
  public ChatStatusResponse joinChat(@Nonnull ChatJoinRequest request) {
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
  public List<String> listEmojis(String regexp, int limit) {
    return listEmojis(regexp != null && !regexp.isBlank() ? new Predicate<>() {
      final Pattern pattern = Pattern.compile(regexp.trim(), Pattern.CASE_INSENSITIVE);

      @Override
      public boolean test(String s) {
        return pattern.matcher(s).find();
      }
    } : any -> true, limit);
  }

  public List<String> listEmojis(Predicate<String> matcher, int limit) {
      return _customEmojiMap.entrySet().stream()
              .filter(entry -> matcher.test(entry.getKey()))
              .limit(limit)
              .sorted(Map.Entry.comparingByKey())
              .map(this::toJson)
              .collect(Collectors.toList());
  }

  private String toJson(Map.Entry<String, String> entry) {
    return JsonObject.of("key", entry.getKey(), "value", entry.getValue()).toString();
  }

  @Override
  public void setCustomEmoji(String key, String url) {
    if (key.startsWith(":") && key.endsWith(":")) {
      _customEmojiMap.put(key, url);
    } else {
      throw new IllegalArgumentException("emoji starts and ends with a colon");
    }
  }

  @Override
  public String getCustomEmoji(String key) {
    return _customEmojiMap.get(key);
  }

  @Override
  public long getPosition() {
    ChatHistory history = _history.get();
    return history != null ? history.position() : -1;
  }

  @Override
  public Instant getEpoch() {
    ChatHistory history = _history.get();
    return history != null ? history.getEpoch() : null;
  }

  @Override
  public boolean isEnabled() {
    return _history.get() != null;
  }
}
