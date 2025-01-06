package net.akaritakai.stream.config;

import net.akaritakai.stream.Main;
import net.sourceforge.argparse4j.annotation.Arg;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class Options {
    @Arg
    public Boolean sslApi;

    public boolean isSslApi() {
        return Boolean.TRUE.equals(sslApi);
    }

    @Arg
    public Boolean sslMedia;

    public boolean isSslMedia() {
        return Boolean.TRUE.equals(sslMedia);
    }

    @Arg(dest = "configFile")
    public ConfigData config;
    @Arg
    public Object command;
    @Arg
    public String apiKey;
    @Arg
    public File emojisFile;
    @Arg
    public Integer port;
    @Arg
    public Integer sslPort;
    @Arg
    public Duration delay;
    @Arg
    public Duration seekTime;
    @Arg
    public Instant startAt;
    @Arg
    public String video;
    @Arg
    public String nickname;
    @Arg
    public String emoji;
    @Arg
    public List<String> text;
    @Arg
    public String host;
}
