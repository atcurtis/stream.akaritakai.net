package net.akaritakai.stream.models.quartz;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Value;
import net.akaritakai.stream.json.ExceptionConverter;
import net.akaritakai.stream.models.telemetry.ExceptionEvent;

@Value
@Builder
@JsonDeserialize(builder = TaskResult.TaskResultBuilder.class)
public class TaskResult {
    @JsonInclude(JsonInclude.Include.NON_NULL) String name;

    String group;

    boolean success;

    String output;
    String outputClassName;

    String stdout;

    //@JsonSerialize(converter = ExceptionConverter.class)
    ExceptionEvent exception;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TaskResultBuilder {
    }
}
