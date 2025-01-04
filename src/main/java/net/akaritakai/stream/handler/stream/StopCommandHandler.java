package net.akaritakai.stream.handler.stream;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.models.stream.request.StreamStopRequest;
import net.akaritakai.stream.scheduling.Utils;
import net.akaritakai.stream.streamer.StreamerMBean;
import org.apache.commons.lang3.Validate;

import static net.akaritakai.stream.config.GlobalNames.streamerName;

/**
 * Handles the "POST /stream/stop" command.
 */
public class StopCommandHandler extends AbstractBlockingHandler<StreamStopRequest> {

  public StopCommandHandler(CheckAuth authCheck, Vertx vertx) {
    super(StreamStopRequest.class, vertx, authCheck);
  }

  protected void validateRequest(StreamStopRequest request) {
    Validate.notNull(request, "request cannot be null");
    Validate.notEmpty(request.getKey(), "key cannot be null/empty");
  }

  protected void handleAuthorized(HttpServerRequest httpRequest, StreamStopRequest request, HttpServerResponse response) {
    executeBlocking(() -> {
      assert request != null;
      Utils.beanProxy(streamerName, StreamerMBean.class)
              .stopStream();
      return null;
    })
            .onSuccess(aVoid -> handleSuccess(response))
            .onFailure(ex -> handleFailure(response, ex));
  }

  private void handleFailure(HttpServerResponse response, Throwable t) {
    handleFailure("Stream cannot be stopped.", response, t);
    response.setStatusCode(409); // Conflict
  }

  private void handleSuccess(HttpServerResponse response) {
    String message = "Stopped the stream";
    handleSuccess(message, TEXT_PLAIN, response);
  }
}
