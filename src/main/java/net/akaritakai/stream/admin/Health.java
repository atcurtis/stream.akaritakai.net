package net.akaritakai.stream.admin;

import net.akaritakai.stream.config.Options;
import net.akaritakai.stream.config.ShutdownAction;
import net.akaritakai.stream.debug.TouchTimer;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;

public class Health extends AbstractCommand{
    public Health(TouchTimer startTimer, Options opt, CompletableFuture<Void> shutdown, Stack<ShutdownAction> shutdownActions) {
        super(startTimer, opt, shutdown, shutdownActions);
    }

    public void run() throws Exception {
        log.info("Health response: {}", IOUtils.toString(uri("/health", null), StandardCharsets.UTF_8));
    }
}
