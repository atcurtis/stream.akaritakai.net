package net.akaritakai.stream.chat.command;

import net.akaritakai.stream.chat.ChatCommand;
import net.akaritakai.stream.models.chat.ChatMessage;
import net.akaritakai.stream.models.chat.ChatMessageType;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;

public class Me implements ChatCommand {
    @Override
    public String command() {
        return "/me";
    }

    @Override
    public ChatMessage execute(ChatSendRequest request, String args, Response response) {
        return response.send(ChatSendRequest.builder()
                        .messageType(ChatMessageType.TEXT)
                        .message(" . o O (  " + args + "  )")
                        .nickname(request.getNickname())
                        .source(request.getSource())
                        .build(),
                null);
    }
}
