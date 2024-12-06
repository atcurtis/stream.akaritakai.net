package net.akaritakai.stream.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.RouterHelper;
import net.akaritakai.stream.handler.chat.*;
import net.akaritakai.stream.scheduling.Utils;
import org.quartz.Scheduler;

import javax.management.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

public class Chat {
    private final Vertx vertx;
    private final RouterHelper router;
    private final ChatManager chatManager;
    private final CheckAuth checkAuth;
    private final ObjectName chatManagerName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Chat(Vertx vertx, RouterHelper router, Scheduler scheduler, CheckAuth checkAuth,
                MBeanServer mBeanServer, ObjectName chatManagerName)
            throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        this.vertx = vertx;
        this.router = router;
        this.chatManager = new ChatManager(scheduler);
        this.checkAuth = checkAuth;
        this.chatManagerName = chatManagerName;

        mBeanServer.registerMBean(chatManager, chatManagerName);
        Utils.set(scheduler, ChatManagerMBean.KEY, chatManagerName);
    }

    public Chat addCustomEmojis(File jsonFile) throws IOException {
        if (jsonFile != null) {
            for (Map.Entry<String, String> entry : objectMapper
                    .readValue(jsonFile, new TypeReference<Map<String, String>>() {
                    }).entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getKey().isBlank()) {
                        throw new IllegalArgumentException("key name can't be blank");
                    }
                    chatManager.setCustomEmoji(":" + entry.getKey() + ":", new URL(entry.getValue()).toString());
                }
            }
        }
        return this;
    }

    public void install() {
        router.registerPostApiHandler("/chat/clear", new ChatClearHandler(chatManagerName, checkAuth, vertx));
        router.registerPostApiHandler("/chat/disable", new ChatDisableHandler(chatManagerName, checkAuth, vertx));
        router.registerPostApiHandler("/chat/enable", new ChatEnableHandler(chatManagerName, checkAuth, vertx));
        router.registerPostApiHandler("/chat/write", new ChatWriteHandler(chatManagerName, checkAuth));
        router.registerPostApiHandler("/chat/emojis", new ChatListEmojisHandler(chatManagerName, checkAuth));
        router.registerPostApiHandler("/chat/emoji", new ChatSetEmojiHandler(chatManagerName, checkAuth));
        router.registerGetHandler("/chat", new ChatClientHandler(vertx, chatManagerName));
    }
}
