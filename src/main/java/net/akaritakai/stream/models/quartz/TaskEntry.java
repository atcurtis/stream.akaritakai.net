package net.akaritakai.stream.models.quartz;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = TaskEntry.TaskEntryBuilder.class)
public class TaskEntry {
    @JsonInclude(JsonInclude.Include.NON_NULL) String name;

    String group;

    @JsonInclude(JsonInclude.Include.NON_NULL) String code;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TaskEntryBuilder {
    }
}
