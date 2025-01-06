package net.akaritakai.stream.admin;

import io.vertx.core.json.JsonObject;
import net.akaritakai.stream.config.Options;
import net.akaritakai.stream.config.ShutdownAction;
import net.akaritakai.stream.debug.TouchTimer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractCommand {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final Options opt;
    private CompletableFuture<Void> shutdown;

    protected AbstractCommand(TouchTimer startTimer, Options opt, CompletableFuture<Void> shutdown, Stack<ShutdownAction> shutdownActions) {
        startTimer.touch("Constructed instance {}", getClass().getName());
        shutdownActions.add(this::onShutdown);
        this.opt = opt;
        this.shutdown = shutdown;
    }

    protected URI uri(String path, String query) throws URISyntaxException {
        return new URI(opt.isSslApi() ? "https" : "http", null, opt.host, opt.isSslApi() ? opt.sslPort : opt.port, path, query, null);
    }

    protected void shutdown() {
        shutdown.complete(null);
    }

    protected void onShutdown() {

    }

    protected String simple(String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri(path, null).toURL().openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("content-type", "application/json");
        try (OutputStream os = connection.getOutputStream()) {
            IOUtils.write(JsonObject.of("key", opt.apiKey ).toString(), os, StandardCharsets.UTF_8);
        }
        return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
    }

}
