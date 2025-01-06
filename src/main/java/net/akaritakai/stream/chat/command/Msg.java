package net.akaritakai.stream.chat.command;

import net.akaritakai.stream.chat.ChatCommand;
import net.akaritakai.stream.handler.Util;
import net.akaritakai.stream.models.chat.ChatMessage;
import net.akaritakai.stream.models.chat.ChatMessageType;
import net.akaritakai.stream.models.chat.ChatTarget;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;

import java.util.Set;

public class Msg implements ChatCommand {
    @Override
    public String command() {
        return "/msg";
    }

    @Override
    public ChatMessage execute(ChatSendRequest request, String args, Response response) {
        String[] target = args.split("\\s+", 2);

        return response.send(ChatSendRequest.builder()
                        .messageType(ChatMessageType.TEXT)
                        .message(target[1])
                        .nickname(request.getNickname() + "\u2794" + target[0])
                        .source(request.getSource())
                        .build(),
                ChatTarget.builder()
                        .nickname(Set.of(target[0], request.getNickname()))
                        .build());
    }
}
