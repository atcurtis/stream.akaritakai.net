package net.akaritakai.stream.config;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public final class GlobalNames {
    private GlobalNames() {
    }

    public static final String ROOT_LOGGER_NAME = ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME;

    public static final String JDBC_URL = "jdbc:sqlite:data.db";

    public static final ObjectName streamerName;
    public static final ObjectName chatManagerName;
    public static final ObjectName scriptManagerName;
    public static final ObjectName scheduleManagerName;
    public static final ObjectName telemetryStoreName;
    public static final ObjectName fortuneStoreName;
    public static final ObjectName stateStoreName;

    static {
        try {
            streamerName = new ObjectName("net.akaritakai.stream:type=Streamer");
            chatManagerName = new ObjectName("net.akaritakai.stream:type=ChatManager");
            scriptManagerName = new ObjectName("net.akaritakai.stream:type=ScriptManager");
            scheduleManagerName = new ObjectName("net.akaritakai.stream:type=ScheduleManager");
            telemetryStoreName = new ObjectName("net.akaritakai.stream:type=TelemetryStore");
            fortuneStoreName = new ObjectName("net.akaritakai.stream:type=FortuneStore");
            stateStoreName = new ObjectName("net.akaritakai.stream:type=StateStore");
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }
}
