package net.akaritakai.stream;

import net.akaritakai.stream.config.GlobalNames;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class InitDB {
    public static void main(String[] args) throws Exception {
        // create a database connection
        try (Connection connection = DriverManager.getConnection(GlobalNames.JDBC_URL);
             Statement statement = connection.createStatement()) {
            initDB(statement);
        }
    }

    public static void initDB(Statement statement) throws SQLException {

        statement.setQueryTimeout(30);  // set timeout to 30 sec.

        Set<String> names = new HashSet<>();

        try (ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
            while (rs.next()) {
                names.add(rs.getString(1).toUpperCase());
            }
        }

        if (!names.contains("QRTZ_JOB_DETAILS")) statement.executeUpdate(
                        "CREATE TABLE QRTZ_JOB_DETAILS\n" +
                        "  (\n" +
                        "    SCHED_NAME VARCHAR(120) NOT NULL,\n" +
                        "    JOB_NAME  VARCHAR(200) NOT NULL,\n" +
                        "    JOB_GROUP VARCHAR(200) NOT NULL,\n" +
                        "    DESCRIPTION VARCHAR(250) NULL,\n" +
                        "    JOB_CLASS_NAME   VARCHAR(250) NOT NULL,\n" +
                        "    IS_DURABLE VARCHAR(1) NOT NULL,\n" +
                        "    IS_NONCONCURRENT VARCHAR(1) NOT NULL,\n" +
                        "    IS_UPDATE_DATA VARCHAR(1) NOT NULL,\n" +
                        "    REQUESTS_RECOVERY VARCHAR(1) NOT NULL,\n" +
                        "    JOB_DATA BLOB NULL,\n" +
                        "    PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)\n" +
                        ")");

        if (!names.contains("QRTZ_TRIGGERS")) statement.executeUpdate(
                "CREATE TABLE QRTZ_TRIGGERS\n" +
                        "  (\n" +
                        "    SCHED_NAME VARCHAR(120) NOT NULL,\n" +
                        "    TRIGGER_NAME VARCHAR(200) NOT NULL,\n" +
                        "    TRIGGER_GROUP VARCHAR(200) NOT NULL,\n" +
                        "    JOB_NAME  VARCHAR(200) NOT NULL,\n" +
                        "    JOB_GROUP VARCHAR(200) NOT NULL,\n" +
                        "    DESCRIPTION VARCHAR(250) NULL,\n" +
                        "    NEXT_FIRE_TIME BIGINT(13) NULL,\n" +
                        "    PREV_FIRE_TIME BIGINT(13) NULL,\n" +
                        "    PRIORITY INTEGER NULL,\n" +
                        "    TRIGGER_STATE VARCHAR(16) NOT NULL,\n" +
                        "    TRIGGER_TYPE VARCHAR(8) NOT NULL,\n" +
                        "    START_TIME BIGINT(13) NOT NULL,\n" +
                        "    END_TIME BIGINT(13) NULL,\n" +
                        "    CALENDAR_NAME VARCHAR(200) NULL,\n" +
                        "    MISFIRE_INSTR SMALLINT(2) NULL,\n" +
                        "    JOB_DATA BLOB NULL,\n" +
                        "    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),\n" +
                        "    FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)\n" +
                        "        REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)\n" +
                        ")");

        if (!names.contains("QRTZ_SIMPLE_TRIGGERS")) statement.executeUpdate(
                        "CREATE TABLE QRTZ_SIMPLE_TRIGGERS\n" +
                        "  (\n" +
                        "    SCHED_NAME VARCHAR(120) NOT NULL,\n" +
                        "    TRIGGER_NAME VARCHAR(200) NOT NULL,\n" +
                        "    TRIGGER_GROUP VARCHAR(200) NOT NULL,\n" +
                        "    REPEAT_COUNT BIGINT(7) NOT NULL,\n" +
                        "    REPEAT_INTERVAL BIGINT(12) NOT NULL,\n" +
                        "    TIMES_TRIGGERED BIGINT(10) NOT NULL,\n" +
                        "    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),\n" +
                        "    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)\n" +
                        "        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)\n" +
                        ")");

        if (!names.contains("QRTZ_CRON_TRIGGERS")) statement.executeUpdate(
                        "CREATE TABLE QRTZ_CRON_TRIGGERS\n" +
                        "  (\n" +
                        "    SCHED_NAME VARCHAR(120) NOT NULL,\n" +
                        "    TRIGGER_NAME VARCHAR(200) NOT NULL,\n" +
                        "    TRIGGER_GROUP VARCHAR(200) NOT NULL,\n" +
                        "    CRON_EXPRESSION VARCHAR(200) NOT NULL,\n" +
                        "    TIME_ZONE_ID VARCHAR(80),\n" +
                        "    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),\n" +
                        "    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)\n" +
                        "        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)\n" +
                        ")");

        if (!names.contains("QRTZ_SIMPROP_TRIGGERS")) statement.executeUpdate(
                "CREATE TABLE QRTZ_SIMPROP_TRIGGERS\n" +
                "  (          \n" +
                "    SCHED_NAME VARCHAR(120) NOT NULL,\n" +
                "    TRIGGER_NAME VARCHAR(200) NOT NULL,\n" +
                "    TRIGGER_GROUP VARCHAR(200) NOT NULL,\n" +
                "    STR_PROP_1 VARCHAR(512) NULL,\n" +
                "    STR_PROP_2 VARCHAR(512) NULL,\n" +
                "    STR_PROP_3 VARCHAR(512) NULL,\n" +
                "    INT_PROP_1 INT NULL,\n" +
                "    INT_PROP_2 INT NULL,\n" +
                "    LONG_PROP_1 BIGINT NULL,\n" +
                "    LONG_PROP_2 BIGINT NULL,\n" +
                "    DEC_PROP_1 NUMERIC(13,4) NULL,\n" +
                "    DEC_PROP_2 NUMERIC(13,4) NULL,\n" +
                "    BOOL_PROP_1 VARCHAR(1) NULL,\n" +
                "    BOOL_PROP_2 VARCHAR(1) NULL,\n" +
                "    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),\n" +
                "    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) \n" +
                "    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)\n" +
                ")");

        if (!names.contains("QRTZ_BLOB_TRIGGERS")) statement.executeUpdate(
                        "CREATE TABLE QRTZ_BLOB_TRIGGERS\n" +
                        "  (\n" +
                        "    SCHED_NAME VARCHAR(120) NOT NULL,\n" +
                        "    TRIGGER_NAME VARCHAR(200) NOT NULL,\n" +
                        "    TRIGGER_GROUP VARCHAR(200) NOT NULL,\n" +
                        "    BLOB_DATA BLOB NULL,\n" +
                        "    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),\n" +
                        "    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)\n" +
                        "        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)\n" +
                        ")");

        if (!names.contains("QRTZ_CALENDARS")) statement.executeUpdate(
                        "CREATE TABLE QRTZ_CALENDARS\n" +
                        "  (\n" +
                        "    SCHED_NAME VARCHAR(120) NOT NULL,\n" +
                        "    CALENDAR_NAME  VARCHAR(200) NOT NULL,\n" +
                        "    CALENDAR BLOB NOT NULL,\n" +
                        "    PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)\n" +
                        ")");

        if (!names.contains("QRTZ_PAUSED_TRIGGER_GRPS")) statement.executeUpdate(
                        "CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS\n" +
                        "  (\n" +
                        "    SCHED_NAME VARCHAR(120) NOT NULL,\n" +
                        "    TRIGGER_GROUP  VARCHAR(200) NOT NULL, \n" +
                        "    PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)\n" +
                        ")");

        if (!names.contains("QRTZ_FIRED_TRIGGERS")) statement.executeUpdate(
                "CREATE TABLE QRTZ_FIRED_TRIGGERS\n" +
                        "  (\n" +
                        "    SCHED_NAME VARCHAR(120) NOT NULL,\n" +
                        "    ENTRY_ID VARCHAR(95) NOT NULL,\n" +
                        "    TRIGGER_NAME VARCHAR(200) NOT NULL,\n" +
                        "    TRIGGER_GROUP VARCHAR(200) NOT NULL,\n" +
                        "    INSTANCE_NAME VARCHAR(200) NOT NULL,\n" +
                        "    FIRED_TIME BIGINT(13) NOT NULL,\n" +
                        "    SCHED_TIME BIGINT(13) NOT NULL,\n" +
                        "    PRIORITY INTEGER NOT NULL,\n" +
                        "    STATE VARCHAR(16) NOT NULL,\n" +
                        "    JOB_NAME VARCHAR(200) NULL,\n" +
                        "    JOB_GROUP VARCHAR(200) NULL,\n" +
                        "    IS_NONCONCURRENT VARCHAR(1) NULL,\n" +
                        "    REQUESTS_RECOVERY VARCHAR(1) NULL,\n" +
                        "    PRIMARY KEY (SCHED_NAME,ENTRY_ID)\n" +
                        ")");

        if (!names.contains("QRTZ_SCHEDULER_STATE")) statement.executeUpdate(
                        "CREATE TABLE QRTZ_SCHEDULER_STATE\n" +
                        "  (\n" +
                        "    SCHED_NAME VARCHAR(120) NOT NULL,\n" +
                        "    INSTANCE_NAME VARCHAR(200) NOT NULL,\n" +
                        "    LAST_CHECKIN_TIME BIGINT(13) NOT NULL,\n" +
                        "    CHECKIN_INTERVAL BIGINT(13) NOT NULL,\n" +
                        "    PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)\n" +
                        ")");

        if (!names.contains("QRTZ_LOCKS")) statement.executeUpdate("\n" +
                "CREATE TABLE QRTZ_LOCKS\n" +
                "  (\n" +
                "    SCHED_NAME VARCHAR(120) NOT NULL,\n" +
                "    LOCK_NAME  VARCHAR(40) NOT NULL, \n" +
                "    PRIMARY KEY (SCHED_NAME,LOCK_NAME)\n" +
                ")");

        if (!names.contains("EMOJIS")) statement.executeUpdate("\n" +
                "CREATE TABLE EMOJIS\n" +
                "  (\n" +
                "    EMOJI_NAME VARCHAR(40) NOT NULL,\n" +
                "    EMOJI_URL  VARCHAR(4000) NOT NULL, \n" +
                "    PRIMARY KEY (EMOJI_NAME)\n" +
                ")");

        if (!names.contains("STATE_STORE")) statement.executeUpdate("\n" +
                "CREATE TABLE STATE_STORE\n" +
                "  (\n" +
                "    STATE_NAME VARCHAR(40) NOT NULL,\n" +
                "    STATE_VALUE BLOB, \n" +
                "    PRIMARY KEY (STATE_NAME)\n" +
                ")");

        if (!names.contains("SCRIPT_STORE")) statement.executeUpdate("\n" +
                "CREATE TABLE SCRIPT_STORE\n" +
                "  (\n" +
                "    SCRIPT_NAME VARCHAR(40) NOT NULL,\n" +
                "    SCRIPT_GROUP VARCHAR(40),\n" +
                "    SCRIPT_TEXT TEXT, \n" +
                "    PRIMARY KEY (SCRIPT_GROUP, SCRIPT_NAME)\n" +
                ")");

    }
}
