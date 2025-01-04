package net.akaritakai.stream.handler.chat;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.chat.ChatManagerMBean;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.models.chat.commands.ChatEnableRequest;
import net.akaritakai.stream.scheduling.Utils;
import org.apache.commons.lang3.Validate;

import javax.management.ObjectName;


/**
 * Handles the "POST /chat/enable" command.
 */
public class ChatEnableHandler extends AbstractBlockingHandler<ChatEnableRequest> {

  private final ObjectName _chat;

  public ChatEnableHandler(ObjectName chat, CheckAuth checkAuth, Vertx vertx) {
    super(ChatEnableRequest.class, vertx, checkAuth);
    _chat = chat;
  }

  @Override
  protected void validateRequest(ChatEnableRequest request) {
    Validate.notNull(request, "request cannot be null");
    Validate.notEmpty(request.getKey(), "key cannot be null/empty");
  }

  protected void handleAuthorized(HttpServerRequest httpRequest, ChatEnableRequest request, HttpServerResponse response) {
    executeBlocking(() -> {
      Utils.beanProxy(_chat, ChatManagerMBean.class).enableChat();
      return null;
    })
            .onSuccess(unused -> handleSuccess(response))
            .onFailure(ex -> handleFailure(response, ex));
  }

  protected void handleFailure(HttpServerResponse response, Throwable t) {
    handleFailure("Chat cannot be enabled.", response, t);
  }

  private void handleSuccess(HttpServerResponse response) {
    String message = "Enabled the chat";
    handleSuccess(message, "text/plain", response);
  }
}
