package net.akaritakai.stream.handler.chat;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.models.chat.request.ChatSetEmojisRequest;
import org.apache.commons.lang3.Validate;

import javax.management.ObjectName;


/**
 * Handles the "POST /chat/clear" command.
 */
public class ChatSetEmojiHandler extends AbstractChatHandler<ChatSetEmojisRequest> {

  public ChatSetEmojiHandler(ObjectName chat, Vertx vertx, CheckAuth checkAuth) {
    super(ChatSetEmojisRequest.class, chat, vertx, checkAuth);
  }

  @Override
  protected void validateRequest(ChatSetEmojisRequest request) {
    Validate.notNull(request, "request cannot be null");
    Validate.notEmpty(request.getKey(), "key cannot be null/empty");
    Validate.notNull(request.getEmoji(), "emoji cannot be null/empty");
    Validate.isTrue(request.getEmoji().getName().startsWith(":"), "name should start with colon");
    Validate.isTrue(request.getEmoji().getName().endsWith(":"), "name should end with colon");
  }

  @Override
  protected void handleAuthorized(HttpServerRequest httpRequest, ChatSetEmojisRequest request, HttpServerResponse response) {
    executeBlocking(() -> {
      chatManager().setCustomEmoji(request.getEmoji().getName(), request.getEmoji().getUrl().toString());
      return null;
    })
            .onSuccess(unused ->  handleSuccess("OK", "text/plain", response))
            .onFailure(ex -> handleFailure(response, ex));
  }

  protected void handleFailure(HttpServerResponse response, Throwable t) {
    handleFailure("Emojis cannot be set.", response, t);
  }
}
