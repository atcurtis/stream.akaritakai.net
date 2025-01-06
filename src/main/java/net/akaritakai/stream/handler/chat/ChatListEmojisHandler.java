package net.akaritakai.stream.handler.chat;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.models.chat.ChatEmojiEntry;
import net.akaritakai.stream.models.chat.Entry;
import net.akaritakai.stream.models.chat.request.ChatListEmojisRequest;
import net.akaritakai.stream.models.chat.response.ChatListEmojisResponse;
import org.apache.commons.lang3.Validate;

import javax.management.ObjectName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Handles the "POST /chat/clear" command.
 */
public class ChatListEmojisHandler extends AbstractChatHandler<ChatListEmojisRequest> {

  public ChatListEmojisHandler(ObjectName chat, Vertx vertx, CheckAuth checkAuth) {
    super(ChatListEmojisRequest.class, chat, vertx, checkAuth);
  }

  @Override
  protected void validateRequest(ChatListEmojisRequest request) {
    Validate.notNull(request, "request cannot be null");
    Validate.notEmpty(request.getKey(), "key cannot be null/empty");
  }

  @Override
  protected void handleAuthorized(HttpServerRequest httpRequest, ChatListEmojisRequest request, HttpServerResponse response) {
    executeBlocking(() -> {
      List<String> entries = chatManager().listEmojis(request.getFilter(), 10);

      List<ChatEmojiEntry> emojiEntries = new ArrayList<>(10);
      entries.stream().map(entry -> (Map.Entry<String, String>) Json.decodeValue(entry, Entry.class))
              .forEach(entry -> emojiEntries.add(ChatEmojiEntry.builder()
                      .name(entry.getKey())
                      .url(toURL(entry.getValue()))
                      .build()));

      ChatListEmojisResponse listResponse = ChatListEmojisResponse.builder()
              .entries(emojiEntries)
              .build();

      return writeValue(listResponse);
    })
            .onSuccess(responseAsString -> handleSuccess(responseAsString, "application/json", response))
            .onFailure(ex -> handleFailure(response, ex));
  }

  private static URL toURL(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  protected void handleFailure(HttpServerResponse response, Throwable t) {
    handleFailure("Emojis cannot be enumerated.", response, t);
  }
}
