package net.akaritakai.stream.models.chat;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Value;
import net.akaritakai.stream.json.*;

import java.net.InetAddress;
import java.util.UUID;


@Value
@Builder
@JsonDeserialize(builder = ChatMessage.ChatMessageBuilder.class)
public class ChatMessage {
  ChatSequence sequence;
  ChatMessageType messageType;
  String nickname;
  String message;
  Long timestamp;
  @JsonSerialize(converter = InetAddressToStringConverter.class)
  InetAddress source;
  ChatTarget target;
  @JsonSerialize(converter = UuidToNullConverter.class)
  UUID uuid;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ChatMessageBuilder implements ChatMessageBuilderMixin {
  }

  private interface ChatMessageBuilderMixin {
    @JsonDeserialize(converter = StringToInetAddressConverter.class)
    ChatMessageBuilder source(InetAddress ipAddress);
  }
}
