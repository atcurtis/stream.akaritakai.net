package net.akaritakai.stream.models.quartz.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.akaritakai.stream.models.quartz.TaskResult;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(builder = ExecuteResponse.ExecuteResponseBuilder.class)
public class ExecuteResponse {

    TaskResult result;

    @JsonPOJOBuilder(withPrefix = "")
    public static class ExecuteResponseBuilder {
    }

}
