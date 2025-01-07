package net.akaritakai.stream.chat.command;

import io.netty.util.internal.StringUtil;
import net.akaritakai.stream.chat.ChatCommand;
import net.akaritakai.stream.chat.FortuneStoreMBean;
import net.akaritakai.stream.models.chat.ChatMessage;
import net.akaritakai.stream.models.chat.ChatMessageType;
import net.akaritakai.stream.models.chat.request.ChatSendRequest;
import net.akaritakai.stream.scheduling.Utils;

import static net.akaritakai.stream.config.GlobalNames.fortuneStoreName;

public class Fortune implements ChatCommand {
    @Override
    public String command() {
        return "/fortune";
    }

    @Override
    public ChatMessage execute(ChatSendRequest request, String args, Response response) {
        FortuneStoreMBean fortuneStore = Utils.beanProxy(fortuneStoreName, FortuneStoreMBean.class);
        return response.send(ChatSendRequest.builder()
                        .messageType(ChatMessageType.TEXT)
                        .message(StringUtil.join("\n", fortuneStore.randomFortune()).toString())
                        .nickname("\uD83E\uDDDE")
                        .source(request.getSource())
                        .build(),
                null);
    }
}
