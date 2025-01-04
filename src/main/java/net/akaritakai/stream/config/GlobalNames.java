package net.akaritakai.stream.config;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public final class GlobalNames {
    private GlobalNames() {
    }

    public static final String ROOT_LOGGER_NAME = ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME;

    public static final String JDBC_URL = "jdbc:sqlite:scheduler.db";

    public static final ObjectName streamerName;
    public static final ObjectName chatManagerName;
    public static final ObjectName scriptManagerName;
    public static final ObjectName scheduleManagerName;

    static {
        try {
            streamerName = new ObjectName("net.akaritakai.stream:type=Streamer");
            chatManagerName = new ObjectName("net.akaritakai.stream:type=ChatManager");
            scriptManagerName = new ObjectName("net.akaritakai.stream:type=ScriptManager");
            scheduleManagerName = new ObjectName("net.akaritakai.stream:type=ScheduleManager");
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }
}
