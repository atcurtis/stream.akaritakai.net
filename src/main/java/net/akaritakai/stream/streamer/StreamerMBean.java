package net.akaritakai.stream.streamer;

import net.akaritakai.stream.models.stream.request.StreamResumeRequest;
import net.akaritakai.stream.models.stream.request.StreamStartRequest;
import net.akaritakai.stream.scheduling.SchedulerAttribute;

import javax.annotation.Nonnull;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface StreamerMBean extends NotificationEmitter {
    SchedulerAttribute<ObjectName> KEY = SchedulerAttribute.instanceOf(StreamerMBean.class.getName(), ObjectName.class);

    default StreamResumeRequest.StreamResumeRequestBuilder streamResumeRequestBuilder() {
        return StreamResumeRequest.builder();
    }

    default StreamStartRequest.StreamStartRequestBuilder streamStartRequestBuilder() {
        return StreamStartRequest.builder();
    }

    String getState();
    void stopStream();
    void resumeStream(@Nonnull StreamResumeRequest streamResumeRequest);
    void pauseStream();
    void startStream(@Nonnull StreamStartRequest streamStartRequest);
    List<String> listStreams(String filter);
    String getStatus();
    boolean isLive();
    String getCurrentPlaylist();
    String getCurrentMediaName();
    Duration getCurrentDuration();
    Instant getCurrentStartTime();
    Instant getCurrentEndTime();

}
