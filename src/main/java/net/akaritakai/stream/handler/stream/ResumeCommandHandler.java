package net.akaritakai.stream.handler.stream;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.models.stream.request.StreamResumeRequest;
import net.akaritakai.stream.scheduling.Utils;
import net.akaritakai.stream.streamer.StreamerMBean;
import org.apache.commons.lang3.Validate;

import static net.akaritakai.stream.config.GlobalNames.streamerName;


/**
 * Handles the "POST /stream/resume" command.
 */
public class ResumeCommandHandler extends AbstractBlockingHandler<StreamResumeRequest> {

  public ResumeCommandHandler(CheckAuth authCheck, Vertx vertx) {
    super(StreamResumeRequest.class, vertx, authCheck);
  }

  @Override
  protected void validateRequest(StreamResumeRequest request) {
    Validate.notNull(request, "request cannot be null");
    Validate.notEmpty(request.getKey(), "key cannot be null/empty");
    Validate.isTrue(request.getDelay() == null || !request.getDelay().isNegative(),
        "delay cannot be negative");
    Validate.isTrue(request.getSeekTime() == null || !request.getSeekTime().isNegative(),
        "seek time cannot be negative");
  }

  protected void handleAuthorized(HttpServerRequest httpRequest, StreamResumeRequest request, HttpServerResponse response) {
    executeBlocking(() -> {
      Utils.beanProxy(streamerName, StreamerMBean.class)
              .resumeStream(request);
      return null;
    })
            .onSuccess(unused -> handleSuccess(response))
            .onFailure(ex -> handleFailure(response, ex));
  }

  private void handleFailure(HttpServerResponse response, Throwable t) {
    handleFailure("Stream cannot be resumed.", response, t);
  }

  private void handleSuccess(HttpServerResponse response) {
    String message = "Resumed the stream";
    handleSuccess(message, TEXT_PLAIN, response);
  }
}
