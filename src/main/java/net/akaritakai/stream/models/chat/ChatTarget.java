package net.akaritakai.stream.models.chat;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Value;
import net.akaritakai.stream.json.InetAddressToStringConverter;
import net.akaritakai.stream.json.UuidToNullConverter;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Value
@Builder
@JsonDeserialize(builder = ChatTarget.ChatTargetBuilder.class)
public class ChatTarget {

    Set<String> nickname;

    @JsonSerialize(converter = InetAddressToStringConverter.class)
    InetAddress source;

    @JsonSerialize(converter = UuidToNullConverter.class)
    UUID uuid;

    @JsonPOJOBuilder(withPrefix = "")
    public static class ChatTargetBuilder {
    }
}
