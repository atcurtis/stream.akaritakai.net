package net.akaritakai.stream.handler.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.chat.ChatManager;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.chat.commands.ChatDisableRequest;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;


/**
 * Handles the "POST /chat/disable" command.
 */
public class ChatDisableHandler extends AbstractHandler<ChatDisableRequest> {
  private static final Logger LOG = LoggerFactory.getLogger(ChatDisableHandler.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Vertx _vertx;
  private final ChatManager _chat;

  public ChatDisableHandler(ChatManager chat, CheckAuth checkAuth, Vertx vertx) {
    super(ChatDisableRequest.class, checkAuth);
    _chat = chat;
    _vertx = vertx;
  }
  @Override
  protected void validateRequest(ChatDisableRequest request) {
    Validate.notNull(request, "request cannot be null");
    Validate.notEmpty(request.getKey(), "key cannot be null/empty");
  }

  protected void handleAuthorized(HttpServerRequest httpRequest, ChatDisableRequest request, HttpServerResponse response) {
    CompletableFuture.runAsync(() -> _chat.disableChat(request))
            .whenComplete((unused, ex) -> {
              _vertx.runOnContext(event -> {
                if (ex == null) {
                  handleSuccess(response);
                } else {
                  handleFailure(response, ex);
                }
              });
            });
  }

  protected void handleFailure(HttpServerResponse response, Throwable t) {
    handleFailure("Chat cannot be disabled.", response, t);
  }

  private void handleSuccess(HttpServerResponse response) {
    String message = "Disabled the chat";
    handleSuccess(message, "text/plain", response);
  }
}
