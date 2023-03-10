package net.akaritakai.stream.models.quartz.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(builder = StatusResponse.StatusResponseBuilder.class)
public class StatusResponse {
    String schedName;

    String schedInst;

    String schedClass;

    boolean isRemote;

    boolean started;

    boolean isInStandbyMode;

    boolean shutdown;

    String startTime;

    int numJobsExec;

    String jsClass;

    boolean jsPersistent;

    boolean jsClustered;

    String tpClass;

    int tpSize;

    String version;

    @JsonPOJOBuilder(withPrefix = "")
    public static class StatusResponseBuilder {
    }

}
