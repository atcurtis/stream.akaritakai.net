package net.akaritakai.stream.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;
import net.akaritakai.stream.models.chat.ChatMessage;
import net.akaritakai.stream.models.chat.ChatSequence;
import net.akaritakai.stream.models.chat.ChatTarget;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChatHistory {
  private static final Logger LOG = LoggerFactory.getLogger(ChatHistory.class);
  private static final int CAPACITY = 500;

  private final Queue<ChatMessage> _queue = new ConcurrentLinkedQueue<>();

  private final ConcurrentMap<String, String> _activeCustomMap = new ConcurrentHashMap<>();

  // Epoch information
  private final Instant _epoch = Instant.now();
  private final AtomicLong _position = new AtomicLong();

  public ChatHistory() {
    LOG.info("Current chat state = {}", this);
  }

  public ChatMessage addMessage(ChatSendRequest request) {
    return addMessage(request, null);
  }

  public ChatMessage addMessage(ChatSendRequest request, ChatTarget target) {
    ChatMessage message;
    if (request.getMessageType() == null) {
      throw new IllegalArgumentException("Malformed request");
    }

    synchronized (this) {
      // Build the message with the next sequence value and append it
      long position = _position.incrementAndGet();
      message = ChatMessage.builder()
          .sequence(ChatSequence.builder()
              .epoch(_epoch)
              .position(position)
              .build())
          .messageType(request.getMessageType())
          .nickname(request.getNickname())
          .message(request.getMessage())
          .timestamp(System.currentTimeMillis())
          .source(request.getSource())
          .target(target)
          .build();
      _queue.add(message);
      LOG.info("addMessage({})", message);
    }

    if (_queue.size() > CAPACITY) {
      _queue.remove();
    }

    LOG.debug("Current chat state = {}", this);
    return message;
  }

  public List<ChatMessage> fetchMessages(Instant epoch, long position) {
    if (!Objects.equals(epoch, _epoch)) {
      // User has an epoch not for this store, so show all the messages
      return new ArrayList<>(_queue);
    }
    return _queue.stream()
        .filter(message -> !Objects.equals(epoch, _epoch) // User hasn't seen this store, so show everything
            || message.getSequence().getPosition() > position) // Show messages after the requested message
        .collect(Collectors.toList());
  }

  public Instant getEpoch() {
    return _epoch;
  }

  public ChatSequence getSequence() {
    return ChatSequence.builder()
        .epoch(_epoch)
        .position(position())
        .build();
  }

  public void setActiveEmoji(String token, String content) {
    _activeCustomMap.put(token, content);
  }

  public boolean isActiveEmoji(String token, String content) {
    return Objects.equals(content, _activeCustomMap.get(token));
  }

  public long position() {
    return _position.longValue();
  }

  @Override
  public String toString() {
    return new JsonObject()
        .put("epoch", _epoch.toEpochMilli())
        .put("position", position())
        .put("messages", _queue.toString())
        .toString();
  }
}
