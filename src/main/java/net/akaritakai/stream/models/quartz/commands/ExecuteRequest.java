package net.akaritakai.stream.models.quartz.commands;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;
import net.akaritakai.stream.models.quartz.TaskEntry;

@Value
@Builder
@JsonDeserialize(builder = ExecuteRequest.ExecuteRequestBuilder.class)
public class ExecuteRequest {

    String key;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    TaskEntry task;

    @JsonPOJOBuilder(withPrefix = "")
    public static class ExecuteRequestBuilder {
    }
}
