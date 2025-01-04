package net.akaritakai.stream.handler.stream;

import com.google.common.base.Throwables;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.exception.StreamStateConflictException;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.stream.request.StreamStartRequest;
import net.akaritakai.stream.scheduling.Utils;
import net.akaritakai.stream.streamer.StreamerMBean;
import org.apache.commons.lang3.Validate;

import static net.akaritakai.stream.config.GlobalNames.*;

/**
 * Handles the "POST /stream/start" command.
 */
public class StartCommandHandler extends AbstractBlockingHandler<StreamStartRequest> {

  public StartCommandHandler(CheckAuth authCheck, Vertx vertx) {
    super(StreamStartRequest.class, vertx, authCheck);
  }

  protected void validateRequest(StreamStartRequest request) {
    Validate.notNull(request, "request cannot be null");
    Validate.notEmpty(request.getKey(), "key cannot be null/empty");
    Validate.notEmpty(request.getName(), "name cannot be null/empty");
    Validate.isTrue(request.getSeekTime() == null || !request.getSeekTime().isNegative(),
        "seek time cannot be negative");
    Validate.isTrue(request.getDelay() == null || !request.getDelay().isNegative(),
        "delay cannot be negative");
  }

  protected void handleAuthorized(HttpServerRequest httpRequest, StreamStartRequest request, HttpServerResponse response) {
    executeBlocking(() -> {
      Utils.beanProxy(streamerName, StreamerMBean.class)
              .startStream(request);
      return null;
    })
            .onSuccess(aVoid -> handleSuccess(response))
            .onFailure(ex -> {
                      if (ex instanceof StreamStateConflictException) {
                        handleStreamAlreadyRunning(response);
                      } else {
                        handleStreamCannotBeLoaded(response, ex);
                      }
                    });
  }

  private void handleStreamAlreadyRunning(HttpServerResponse response) {
    String message = "Stream is already running";
    handleResponse(409, message, TEXT_PLAIN, response);
  }

  private void handleStreamCannotBeLoaded(HttpServerResponse response, Throwable e) {
    String message = "Stream cannot be started. Reason:\n" + Throwables.getStackTraceAsString(e);
    handleResponse(404, message, TEXT_PLAIN, response);
  }

  private void handleSuccess(HttpServerResponse response) {
    String message = "Started the stream";
    handleSuccess(message, TEXT_PLAIN, response);
  }
}
