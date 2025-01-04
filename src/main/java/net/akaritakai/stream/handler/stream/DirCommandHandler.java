package net.akaritakai.stream.handler.stream;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.stream.StreamEntry;
import net.akaritakai.stream.models.stream.request.StreamDirRequest;
import net.akaritakai.stream.models.stream.response.StreamDirResponse;
import net.akaritakai.stream.scheduling.Utils;
import net.akaritakai.stream.streamer.StreamerMBean;
import org.apache.commons.lang3.Validate;

import java.util.Comparator;
import java.util.stream.Collectors;

import static net.akaritakai.stream.config.GlobalNames.*;

public class DirCommandHandler extends AbstractHandler<StreamDirRequest> {

    private final Vertx _vertx;

    public DirCommandHandler(CheckAuth checkAuth, Vertx vertx) {
        super(StreamDirRequest.class, checkAuth);
        _vertx = vertx;
    }

    protected void validateRequest(StreamDirRequest request) {
        Validate.notNull(request, "request cannot be null");
        Validate.notEmpty(request.getKey(), "key cannot be null/empty");
    }

    protected void handleAuthorized(HttpServerRequest httpRequest, StreamDirRequest request, HttpServerResponse response) {
        _vertx
                .executeBlocking(() -> OBJECT_MAPPER.writeValueAsString(
                        StreamDirResponse.builder()
                                .entries(Utils.beanProxy(streamerName, StreamerMBean.class)
                                        .listStreams(request.getFilter()).stream()
                                        .map(entry -> readValue(entry, StreamEntry.class))
                                        .sorted(Comparator.comparing(StreamEntry::getName))
                                        .collect(Collectors.toList()))
                                .build())
                )
                .onSuccess(list -> handleSuccess(list, "application/json", response))
                .onFailure(ex -> handleFailure("Directory error", response, ex));
    }
}
