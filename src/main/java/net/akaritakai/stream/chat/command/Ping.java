package net.akaritakai.stream.chat.command;

import net.akaritakai.stream.chat.ChatCommand;
import net.akaritakai.stream.handler.Util;
import net.akaritakai.stream.models.chat.ChatMessage;
import net.akaritakai.stream.models.chat.ChatMessageType;
import net.akaritakai.stream.models.chat.ChatTarget;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;

public class Ping implements ChatCommand {
    @Override
    public String command() {
        return "/ping";
    }

    @Override
    public ChatMessage execute(ChatSendRequest request, String args, Response response) {
        return response.send(ChatSendRequest.builder()
                        .messageType(ChatMessageType.TEXT)
                        .message("pong!")
                        .nickname("\uD83E\uDD16")
                        .source(Util.ANY)
                        .build(),
                ChatTarget.builder()
                        .uuid(request.getUuid())
                        .build());
    }
}
