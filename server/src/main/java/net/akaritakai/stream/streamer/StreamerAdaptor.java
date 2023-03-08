package net.akaritakai.stream.streamer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.akaritakai.stream.models.stream.StreamState;
import net.akaritakai.stream.models.stream.request.StreamPauseRequest;
import net.akaritakai.stream.models.stream.request.StreamResumeRequest;
import net.akaritakai.stream.models.stream.request.StreamStartRequest;
import net.akaritakai.stream.models.stream.request.StreamStopRequest;

public class StreamerAdaptor {
    final StreamerMBean mxBean;
    final ObjectMapper objectMapper = new ObjectMapper();

    public StreamerAdaptor(StreamerMBean mxBean) {
        this.mxBean = mxBean;
    }

    public StreamState getState() {
        try {
            return objectMapper.readValue(mxBean.getState(), StreamState.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopStream(StreamStopRequest request) {
        try {
            mxBean.stopStream(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    public void resumeStream(StreamResumeRequest request) {
        try {
            mxBean.resumeStream(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    public void pauseStream(StreamPauseRequest request) {
        try {
            mxBean.pauseStream(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void startStream(StreamStartRequest request) {
        try {
            mxBean.startStream(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
