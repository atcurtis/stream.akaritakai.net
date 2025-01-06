package net.akaritakai.stream.handler.chat;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.Util;
import net.akaritakai.stream.models.chat.commands.ChatWriteRequest;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;
import org.apache.commons.lang3.Validate;

import javax.management.ObjectName;


/**
 * Handles the "POST /chat/cmd" command.
 */
public class ChatCmdHandler extends AbstractChatHandler<ChatWriteRequest> {

  public ChatCmdHandler(ObjectName chat, Vertx vertx, CheckAuth checkAuth) {
    super(ChatWriteRequest.class, chat, vertx, checkAuth);
  }

  @Override
  protected void validateRequest(ChatWriteRequest request) {
    Validate.notNull(request, "request cannot be null");
    Validate.notEmpty(request.getKey(), "key cannot be null/empty");
  }

  protected void handleAuthorized(HttpServerRequest httpRequest, ChatWriteRequest request, HttpServerResponse response) {
    executeBlocking(() -> {
      ChatSendRequest sendRequest = ChatSendRequest.builder()
              .messageType(request.getMessageType())
              .nickname(request.getNickname())
              .message(request.getMessage())
              .source(Util.getIpAddressFromRequest(httpRequest))
              .build();
      chatManager().sendCommand(sendRequest);
      return null;
    })
            .onSuccess(unused -> handleSuccess(response))
            .onFailure(ex -> handleFailure(response, ex));
  }

  protected void handleFailure(HttpServerResponse response, Throwable t) {
    handleFailure("Chat write failed.", response, t);
  }

  private void handleSuccess(HttpServerResponse response) {
    String message = "OK";
    handleSuccess(message, "text/plain", response);
  }
}
