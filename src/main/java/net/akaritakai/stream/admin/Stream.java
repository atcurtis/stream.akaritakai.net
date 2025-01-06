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

public class Stream extends AbstractCommand{
    public Stream(TouchTimer startTimer, Options opt, CompletableFuture<Void> shutdown, Stack<ShutdownAction> shutdownActions) {
        super(startTimer, opt, shutdown, shutdownActions);
    }

    public void stop() throws Exception {
        log.info("Stop response: {}", simple("/stream/stop"));
    }

    public void pause() throws Exception {
        log.info("Enable response: {}", simple("/stream/pause"));
    }

    public void start() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri("/stream/start", null).toURL().openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("content-type", "application/json");
        try (OutputStream os = connection.getOutputStream()) {
            JsonObject request = JsonObject.of(
                    "key", opt.apiKey,
                    "name", opt.video);
            if (opt.delay != null) {
                request.put("delay", opt.delay.toMillis());
            }
            if (opt.seekTime != null) {
                request.put("seekTime", opt.delay.toMillis());
            }
            if (opt.startAt != null) {
                request.put("startAt", opt.startAt.toEpochMilli());
            }
            IOUtils.write(request.toString(), os, StandardCharsets.UTF_8);
        }
        log.info("Start response: {}", IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8));
    }

    public void resume() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri("/stream/resume", null).toURL().openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("content-type", "application/json");
        try (OutputStream os = connection.getOutputStream()) {
            JsonObject request = JsonObject.of(
                    "key", opt.apiKey);
            if (opt.delay != null) {
                request.put("delay", opt.delay.toMillis());
            }
            if (opt.seekTime != null) {
                request.put("seekTime", opt.delay.toMillis());
            }
            if (opt.startAt != null) {
                request.put("startAt", opt.startAt.toEpochMilli());
            }
            IOUtils.write(request.toString(), os, StandardCharsets.UTF_8);
        }
        log.info("Start response: {}", IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8));
    }

}
