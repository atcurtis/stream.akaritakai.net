package net.akaritakai.stream.chat;

import net.akaritakai.stream.models.chat.ChatMessage;
import net.akaritakai.stream.models.chat.ChatTarget;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;

public interface ChatCommand {

    String command();

    ChatMessage execute(ChatSendRequest request, String args, Response response);

    interface Response {
        ChatMessage send(ChatSendRequest request, ChatTarget target);
    }
}
