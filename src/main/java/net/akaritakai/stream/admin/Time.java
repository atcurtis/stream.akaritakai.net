package net.akaritakai.stream.admin;

import net.akaritakai.stream.config.Options;
import net.akaritakai.stream.config.ShutdownAction;
import net.akaritakai.stream.debug.TouchTimer;
import org.apache.commons.io.IOUtils;
import org.joda.time.Instant;

import java.nio.charset.StandardCharsets;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;

public class Time extends AbstractCommand{
    public Time(TouchTimer startTimer, Options opt, CompletableFuture<Void> shutdown, Stack<ShutdownAction> shutdownActions) {
        super(startTimer, opt, shutdown, shutdownActions);
    }

    public void run() throws Exception {
        log.info("Time response: {}", Instant.ofEpochMilli(Long.parseUnsignedLong(IOUtils.toString(uri("/time", null), StandardCharsets.UTF_8))));
    }
}
