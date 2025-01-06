package net.akaritakai.stream.handler.stream;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.models.stream.request.StreamPauseRequest;
import net.akaritakai.stream.scheduling.Utils;
import net.akaritakai.stream.streamer.StreamerMBean;
import org.apache.commons.lang3.Validate;

import static net.akaritakai.stream.config.GlobalNames.streamerName;

/**
 * Handles the "POST /stream/pause" command.
 */
public class PauseCommandHandler extends AbstractBlockingHandler<StreamPauseRequest> {

  public PauseCommandHandler(CheckAuth checkAuth, Vertx vertx) {
    super(StreamPauseRequest.class, vertx, checkAuth);
  }

  protected void validateRequest(StreamPauseRequest request) {
    Validate.notNull(request, "request cannot be null");
    Validate.notEmpty(request.getKey(), "key cannot be null/empty");
  }

  protected void handleAuthorized(HttpServerRequest httpRequest, StreamPauseRequest request, HttpServerResponse response) {
    executeBlocking(() -> {
      assert request != null;
      Utils.beanProxy(streamerName, StreamerMBean.class)
              .pauseStream();
      return null;
    })
            .onSuccess(unused -> handleSuccess(response))
            .onFailure(ex -> handleFailure(response, ex));
  }

  private void handleFailure(HttpServerResponse response, Throwable t) {
    handleFailure("Stream cannot be paused.", response, t);
  }

  private void handleSuccess(HttpServerResponse response) {
    String message = "Paused the stream";
    handleSuccess(message, TEXT_PLAIN, response);
  }
}
