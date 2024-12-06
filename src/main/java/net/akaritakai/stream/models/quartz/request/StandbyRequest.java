package net.akaritakai.stream.models.quartz.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = StandbyRequest.StandbyRequestBuilder.class)
public class StandbyRequest {
    String key;

    @JsonPOJOBuilder(withPrefix = "")
    public static class StandbyRequestBuilder {
    }
}
