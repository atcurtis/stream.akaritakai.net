package net.akaritakai.stream.models.telemetry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = StackTraceEntry.StackTraceEntryBuilder.class)
public class StackTraceEntry {

    String methodName;
    String fileName;
    int lineNumber;
    int columnNumber;

    @JsonPOJOBuilder(withPrefix = "")
    public static class StackTraceEntryBuilder {
    }
}
