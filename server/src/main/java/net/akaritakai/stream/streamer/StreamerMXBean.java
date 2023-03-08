package net.akaritakai.stream.streamer;

import net.akaritakai.stream.models.stream.StreamState;
import net.akaritakai.stream.models.stream.request.StreamPauseRequest;
import net.akaritakai.stream.models.stream.request.StreamResumeRequest;
import net.akaritakai.stream.models.stream.request.StreamStartRequest;
import net.akaritakai.stream.models.stream.request.StreamStopRequest;

import javax.management.NotificationEmitter;

public interface StreamerMXBean extends NotificationEmitter {
    StreamState getState();
    void stopStream(StreamStopRequest request);
    void resumeStream(StreamResumeRequest request);
    void pauseStream(StreamPauseRequest request);
    void startStream(StreamStartRequest request);
}
