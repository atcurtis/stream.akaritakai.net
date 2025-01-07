package net.akaritakai.stream.chat.command;

import net.akaritakai.stream.chat.ChatCommand;
import net.akaritakai.stream.handler.Util;
import net.akaritakai.stream.models.chat.ChatMessage;
import net.akaritakai.stream.models.chat.ChatMessageType;
import net.akaritakai.stream.models.chat.ChatTarget;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;
import net.akaritakai.stream.scheduling.Utils;
import net.akaritakai.stream.telemetry.TelemetryStoreMBean;

import static net.akaritakai.stream.config.GlobalNames.telemetryStoreName;

public class Online implements ChatCommand {
    @Override
    public String command() {
        return "/online";
    }

    @Override
    public ChatMessage execute(ChatSendRequest request, String args, Response response) {
        TelemetryStoreMBean telemetryStore = Utils.beanProxy(telemetryStoreName, TelemetryStoreMBean.class);

        return response.send(ChatSendRequest.builder()
                        .messageType(ChatMessageType.TEXT)
                        .message("There are "
                                + telemetryStore.size()
                                + " online")
                        .nickname("\uD83E\uDD16")
                        .source(Util.ANY)
                        .build(),
                ChatTarget.builder()
                        .uuid(request.getUuid())
                        .build());
    }
}
