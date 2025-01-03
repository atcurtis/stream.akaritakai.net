package net.akaritakai.stream.handler.chat;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.chat.ChatManagerMBean;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.chat.request.ChatSetEmojisRequest;
import net.akaritakai.stream.scheduling.Utils;
import org.apache.commons.lang3.Validate;

import javax.management.ObjectName;


/**
 * Handles the "POST /chat/clear" command.
 */
public class ChatSetEmojiHandler extends AbstractHandler<ChatSetEmojisRequest> {

  private final ChatManagerMBean _chat;

  public ChatSetEmojiHandler(ObjectName chat, CheckAuth checkAuth) {
    super(ChatSetEmojisRequest.class, checkAuth);
    _chat = Utils.beanProxy(chat, ChatManagerMBean.class);
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
    try {
      _chat.setCustomEmoji(request.getEmoji().getName(), request.getEmoji().getUrl().toString());

      handleSuccess("OK", "text/plain", response);

    } catch (Throwable t) {
      handleFailure(response, t);
    }
  }

  protected void handleFailure(HttpServerResponse response, Throwable t) {
    handleFailure("Emojis cannot be set.", response, t);
  }
}
