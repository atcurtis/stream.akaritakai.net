package net.akaritakai.stream.admin;

import io.vertx.core.json.JsonObject;
import net.akaritakai.stream.config.Options;
import net.akaritakai.stream.config.ShutdownAction;
import net.akaritakai.stream.debug.TouchTimer;
import org.apache.commons.io.IOUtils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;

public class Chat extends AbstractCommand{
    public Chat(TouchTimer startTimer, Options opt, CompletableFuture<Void> shutdown, Stack<ShutdownAction> shutdownActions) {
        super(startTimer, opt, shutdown, shutdownActions);
    }

    public void clear() throws Exception {
        log.info("Clear response: {}", simple("/chat/clear"));
    }

    public void enable() throws Exception {
        log.info("Enable response: {}", simple("/chat/enable"));
    }

    public void disable() throws Exception {
        log.info("Disable response: {}", simple("/chat/disable"));
    }

    private String writeOrCmd(String path, String nick, String text) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri(path, null).toURL().openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("content-type", "application/json");
        try (OutputStream os = connection.getOutputStream()) {
            IOUtils.write(JsonObject.of(
                    "key", opt.apiKey,
                    "messageType", "TEXT",
                    "nickname", nick,
                    "message", text
            ).toString(), os, StandardCharsets.UTF_8);
        }
        return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);

    }

    public void write() throws Exception {
        log.info("Write response: {}", writeOrCmd("/chat/write",
                opt.nickname, String.join(" ", opt.text)));
    }

    public void cmd() throws Exception {
        log.info("Cmd response: {}", writeOrCmd("/chat/cmd",
                opt.nickname, String.join(" ", opt.text)));
    }

}
