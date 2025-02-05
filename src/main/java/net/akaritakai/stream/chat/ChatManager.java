package net.akaritakai.stream.chat;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
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
import net.akaritakai.stream.models.chat.ChatTarget;
import net.akaritakai.stream.models.chat.request.ChatJoinRequest;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;
import net.akaritakai.stream.models.chat.response.ChatStatusResponse;
import net.akaritakai.stream.scheduling.ScheduleManagerMBean;
import net.akaritakai.stream.scheduling.Utils;
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

  private final EmojiStore _emojiStore;

  private final AtomicInteger _sequenceNumber = new AtomicInteger();
  private final ConcurrentMap<String, ChatCommand> _commands = new ConcurrentHashMap<>();

  public ChatManager(TouchTimer startTimer, EmojiStore emojiStore, ConfigData config) {
    _emojiStore = emojiStore;
  }

  public static void register(Vertx vertx, RouterHelper router, CheckAuth checkAuth, ObjectName chatManagerName) {
    router.registerPostApiHandler("/chat/clear", new ChatClearHandler(chatManagerName, checkAuth, vertx));
    router.registerPostApiHandler("/chat/disable", new ChatDisableHandler(chatManagerName, checkAuth, vertx));
    router.registerPostApiHandler("/chat/enable", new ChatEnableHandler(chatManagerName, checkAuth, vertx));
    router.registerPostApiHandler("/chat/write", new ChatWriteHandler(chatManagerName, vertx, checkAuth));
    router.registerPostApiHandler("/chat/cmd", new ChatCmdHandler(chatManagerName, vertx, checkAuth));
    router.registerPostApiHandler("/chat/emojis", new ChatListEmojisHandler(chatManagerName, vertx, checkAuth));
    router.registerPostApiHandler("/chat/emoji", new ChatSetEmojiHandler(chatManagerName, vertx, checkAuth));
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
  public void sendCommand(@Nonnull ChatSendRequest request) {
    ChatHistory history = _history.get();

    if (history == null) {
      throw new IllegalStateException("Chat is not enabled");
    }

    try {
      String[] cmd = request.getMessage().split("\\s+", 2);
      LOG.info("Command sent: {} {} ... \"{}\"", cmd[0], request.getSource(), request.getMessage());
      if (!cmd[0].startsWith("/")) {
        throw new IllegalStateException();
      }
      ChatCommand command = _commands.get(cmd[0]);
      if (command == null) {
        try {
          String className = cmd[0].substring(1, 2).toUpperCase() + cmd[0].substring(2).toLowerCase();
          Class<? extends ChatCommand> cmdClass = Class
                  .forName(getClass().getPackageName() + ".command." + className)
                  .asSubclass(ChatCommand.class);
          command = cmdClass.getConstructor().newInstance();
          if (!command.command().equalsIgnoreCase(cmd[0])) {
            command = null;
          } else {
            _commands.putIfAbsent(command.command(), command);
          }
        } catch (ClassNotFoundException ex) {
          LOG.debug("Not found command: {}", cmd[0], ex);
        } catch (Exception ex) {
          LOG.warn("Failed to instantiate command: {}", cmd[0], ex);
        }
      }

      ChatMessage message;

      if (command != null) {
        message = command.execute(request, cmd.length == 2 ? cmd[1] : "", (chatSendRequest, chatTarget) -> {
          scanForEmoji(history, chatSendRequest);
          return history.addMessage(chatSendRequest, chatTarget);
        });
      } else {
        message = history.addMessage(
                ChatSendRequest.builder()
                        .messageType(ChatMessageType.TEXT)
                        .message("Unknown command: " + cmd[0])
                        .nickname("\uD83E\uDD16")
                        .source(Util.ANY)
                        .build(),
                ChatTarget.builder()
                        .uuid(request.getUuid())
                        .build());
      }

      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
              "sendMessage", "Message", ChatMessage.class.getName(), null, message);
      sendNotification(n);
      return;
    } catch (Exception ex) {
      LOG.warn("Exception processing command {}", request, ex);
      ChatMessage message = history.addMessage(
              ChatSendRequest.builder()
                      .messageType(ChatMessageType.TEXT)
                      .message("Error: " + ex.getClass().getSimpleName())
                      .nickname("\uD83E\uDD16")
                      .source(Util.ANY)
                      .build(),
              ChatTarget.builder()
                      .uuid(request.getUuid())
                      .build());
      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
              "sendMessage", "Message", ChatMessage.class.getName(), null, message);
      sendNotification(n);
    }
  }

  private void scanForEmoji(ChatHistory currentHistory, ChatSendRequest request) {
    StringTokenizer st = new StringTokenizer(request.getMessage());
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (!EmojiStore.isCustomEmoji(token)) {
        continue;
      }
      Emoji emoji = _emojiStore.findCustomEmoji(token);
      if (emoji != null && !currentHistory.isActiveEmoji(emoji.name, emoji.url)) {
        sendMessage(ChatSendRequest.builder()
                .messageType(ChatMessageType.EMOJI)
                .nickname(emoji.name)
                .message(emoji.url)
                .source(Util.ANY)
                .build());
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
        scanForEmoji(currentHistory, request);
      }
      ChatMessage message = currentHistory.addMessage(request);
      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
              "sendMessage", "Message", ChatMessage.class.getName(), null, message);
      sendNotification(n);
      Map<String, String> jobDataMap = new HashMap<>();
      jobDataMap.put("type", String.valueOf(request.getMessageType()));
      jobDataMap.put("nick", request.getNickname());
      jobDataMap.put("message", request.getMessage());
      jobDataMap.put("source", Optional.ofNullable(request.getSource()).map(InetAddress::getHostAddress).orElse(null));
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
    return _emojiStore.listEmojis(regexp, limit).stream().map(Emoji::toString).collect(Collectors.toList());
  }

  @Override
  public void setCustomEmoji(String key, String url) {
    _emojiStore.setCustomEmoji(key, url);
  }

  @Override
  public String getCustomEmoji(String key) {
    Emoji emoji = _emojiStore.findCustomEmoji(key);
    return emoji != null ? emoji.url : null;
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
