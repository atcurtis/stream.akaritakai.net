package net.akaritakai.stream.config;

public interface ShutdownAction {
    void call() throws Exception;
}
