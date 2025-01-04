package net.akaritakai.stream.handler.chat;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.chat.ChatManagerMBean;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.models.chat.commands.ChatClearRequest;
import net.akaritakai.stream.scheduling.Utils;
import org.apache.commons.lang3.Validate;

import javax.management.ObjectName;


/**
 * Handles the "POST /chat/clear" command.
 */
public class ChatClearHandler extends AbstractBlockingHandler<ChatClearRequest> {

  private final ObjectName _chat;

  public ChatClearHandler(ObjectName chat, CheckAuth checkAuth, Vertx vertx) {
    super(ChatClearRequest.class, vertx, checkAuth);
    _chat = chat;
  }

  @Override
  protected void validateRequest(ChatClearRequest request) {
    Validate.notNull(request, "request cannot be null");
    Validate.notEmpty(request.getKey(), "key cannot be null/empty");
  }

  @Override
  protected void handleAuthorized(HttpServerRequest httpRequest, ChatClearRequest request, HttpServerResponse response) {
    executeBlocking(() -> {
      Utils.beanProxy(_chat, ChatManagerMBean.class).clearChat();
      return null;
    })
            .onSuccess(unused -> handleSuccess(response))
            .onFailure(ex -> handleFailure(response, ex));
  }

  protected void handleFailure(HttpServerResponse response, Throwable t) {
    handleFailure("Chat cannot be cleared.", response, t);
  }

  private void handleSuccess(HttpServerResponse response) {
    String message = "Cleared the chat";
    handleSuccess(message, "text/plain", response);
  }
}
