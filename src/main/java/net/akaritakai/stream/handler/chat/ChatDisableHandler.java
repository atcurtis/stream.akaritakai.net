package net.akaritakai.stream.handler.chat;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.models.chat.commands.ChatDisableRequest;
import org.apache.commons.lang3.Validate;

import javax.management.ObjectName;


/**
 * Handles the "POST /chat/disable" command.
 */
public class ChatDisableHandler extends AbstractChatHandler<ChatDisableRequest> {

  public ChatDisableHandler(ObjectName chat, CheckAuth checkAuth, Vertx vertx) {
    super(ChatDisableRequest.class, chat, vertx, checkAuth);
  }

  @Override
  protected void validateRequest(ChatDisableRequest request) {
    Validate.notNull(request, "request cannot be null");
    Validate.notEmpty(request.getKey(), "key cannot be null/empty");
  }

  protected void handleAuthorized(HttpServerRequest httpRequest, ChatDisableRequest request, HttpServerResponse response) {
    executeBlocking(() -> {
      chatManager().disableChat();
      return null;
    })
            .onSuccess(unused -> handleSuccess(response))
            .onFailure(ex -> handleFailure(response, ex));
  }

  protected void handleFailure(HttpServerResponse response, Throwable t) {
    handleFailure("Chat cannot be disabled.", response, t);
  }

  private void handleSuccess(HttpServerResponse response) {
    String message = "Disabled the chat";
    handleSuccess(message, "text/plain", response);
  }
}
