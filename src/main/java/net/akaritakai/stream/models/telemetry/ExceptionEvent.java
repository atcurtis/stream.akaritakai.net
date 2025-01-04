package net.akaritakai.stream.models.telemetry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Value;
import net.akaritakai.stream.json.ExceptionConverter;
import net.akaritakai.stream.json.StackTraceConverter;

@Value
@Builder
@JsonDeserialize(builder = ExceptionEvent.ExceptionEventBuilder.class)
public class ExceptionEvent {

    String message;

    //@JsonSerialize(converter = ExceptionConverter.class)
    ExceptionEvent cause;

    //@JsonSerialize(converter = StackTraceConverter.class)
    StackTraceEntry[] stackTrace;

    @JsonPOJOBuilder(withPrefix = "")
    public static class ExceptionEventBuilder {
    }
}
