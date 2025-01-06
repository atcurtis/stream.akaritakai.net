package net.akaritakai.stream.chat;

import io.vertx.core.json.JsonObject;

import java.util.Objects;

public final class Emoji {
    final String name;
    final String url;

    public Emoji(String name, String url) {
        this.name = Objects.requireNonNull(name);
        this.url = Objects.requireNonNull(url);
    }

    public String toString() {
        return JsonObject.of("key", name, "value", url).toString();
    }
}
