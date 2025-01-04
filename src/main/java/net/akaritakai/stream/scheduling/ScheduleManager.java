package net.akaritakai.stream.scheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.InitDB;
import net.akaritakai.stream.chat.ChatManagerMBean;
import net.akaritakai.stream.config.Config;
import net.akaritakai.stream.config.ConfigData;
import net.akaritakai.stream.config.ShutdownAction;
import net.akaritakai.stream.debug.TouchTimer;
import net.akaritakai.stream.handler.RouterHelper;
import net.akaritakai.stream.handler.quartz.*;
import net.akaritakai.stream.models.quartz.JobEntry;
import net.akaritakai.stream.models.quartz.KeyEntry;
import net.akaritakai.stream.models.quartz.TriggerEntry;
import net.akaritakai.stream.models.quartz.response.StatusResponse;
import net.akaritakai.stream.scheduling.jobs.*;
import net.akaritakai.stream.script.ScriptManagerMBean;
import net.akaritakai.stream.streamer.StreamerMBean;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.JDBC;

import javax.management.NotificationBroadcasterSupport;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static net.akaritakai.stream.config.GlobalNames.*;
import static net.akaritakai.stream.scheduling.Utils.*;

public class ScheduleManager extends NotificationBroadcasterSupport implements ScheduleManagerMBean {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduleManager.class);

    private final Scheduler _scheduler;

    public ScheduleManager(TouchTimer timer) throws Exception {
        _scheduler = createFactory().getScheduler();
        timer.touch("scheduler created");
    }

    public static SchedulerFactory createFactory() throws Exception {
        StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Properties schedulerProperties = new Properties();
        schedulerProperties.setProperty("org.quartz.threadPool.class", org.quartz.simpl.SimpleThreadPool.class.getName());
        schedulerProperties.setProperty("org.quartz.threadPool.threadCount", "4");
        schedulerProperties.setProperty("org.quartz.jobStore.class", SqlLiteJobStoreTX.class.getName());
        schedulerProperties.setProperty("org.quartz.jobStore.driverDelegateClass", SqlLiteJDBCDelegate.class.getName());
        schedulerProperties.setProperty("org.quartz.jobStore.dataSource", "myDS");
        schedulerProperties.setProperty("org.quartz.jobStore.useProperties", "true");
        schedulerProperties.setProperty("org.quartz.dataSource.myDS.driver", JDBC.class.getName());
        schedulerProperties.setProperty("org.quartz.dataSource.myDS.URL", JDBC_URL);
        schedulerProperties.setProperty("org.quartz.scheduler.jmx.export", "true");
        schedulerProperties.setProperty("org.quartz.scheduler.jmx.objectName", "net.akaritakai.stream:type=Scheduler");

        try (Connection connection = DriverManager.getConnection(schedulerProperties.getProperty("org.quartz.dataSource.myDS.URL"));
             ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM QRTZ_LOCKS")) {
            rs.next();
            LOG.info("Database already initialized");
        } catch (SQLException ex) {
            LOG.warn("Attempting to reinitialize the database");
            InitDB.main(new String[0]);
        }

        schedulerFactory.initialize(schedulerProperties);

        return schedulerFactory;
    }

    public static void setup(Scheduler scheduler) throws SchedulerException {
        if (!scheduler.checkExists(JobKey.jobKey("ChatEnable"))) {
            scheduler.addJob(JobBuilder.newJob(ChatEnableJob.class).withIdentity("ChatEnable").storeDurably().build(), false);
        }

        if (!scheduler.checkExists(JobKey.jobKey("ChatDisable"))) {
            scheduler.addJob(JobBuilder.newJob(ChatDisableJob.class).withIdentity("ChatDisable").storeDurably().build(), false);
        }

        if (!scheduler.checkExists(JobKey.jobKey("ChatSend"))) {
            scheduler.addJob(JobBuilder.newJob(ChatSendJob.class).withIdentity("ChatSend").storeDurably().build(), false);
        }

        if (!scheduler.checkExists(JobKey.jobKey("StreamPlay"))) {
            scheduler.addJob(JobBuilder.newJob(StreamPlayJob.class).withIdentity("StreamPlay").storeDurably().build(), false);
        }

        if (!scheduler.checkExists(JobKey.jobKey("StreamStop"))) {
            scheduler.addJob(JobBuilder.newJob(StreamStopJob.class).withIdentity("StreamStop").storeDurably().build(), false);
        }
    }

    public static void register(Vertx vertx, RouterHelper router, CheckAuth auth) {
        router.registerPostApiHandler("/quartz/execute", new ExecuteHandler(vertx, auth));
        router.registerPostApiHandler("/quartz/status", new StatusHandler(vertx, auth));
        router.registerPostApiHandler("/quartz/standby", new StandbyHandler(vertx, auth));
        router.registerPostApiHandler("/quartz/pauseAll", new PauseAllHandler(vertx, auth));
        router.registerPostApiHandler("/quartz/resumeAll", new ResumeAllHandler(vertx, auth));

        router.registerPostApiHandler("/quartz/jobs", new JobsHandler(vertx, auth));
        router.registerPostApiHandler("/quartz/triggers", new TriggersHandler(vertx, auth));
    }

    public Scheduler scheduler() {
        return _scheduler;
    }

    @Override
    public void triggerIfExists(String message, String chat, Map<String, String> jobDataMap) {
        Utils.triggerIfExists(_scheduler, message, chat, new JobDataMap(jobDataMap));
    }

    @Override
    public void triggerIfExists(String message, String chat) {
        Utils.triggerIfExists(_scheduler, message, chat);
    }

    @Override
    public void triggerIfExists(String message) {
        Utils.triggerIfExists(_scheduler, message);
    }

    @Override
    public StatusResponse getMetaData() throws SchedulerException {
        SchedulerMetaData metadata =  _scheduler.getMetaData();
        return StatusResponse.builder()
                .schedName(metadata.getSchedulerName())
                .schedInst(metadata.getSchedulerInstanceId())
                .schedClass(metadata.getSchedulerClass().getName())
                .isRemote(metadata.isSchedulerRemote())
                .started(metadata.isStarted())
                .isInStandbyMode(metadata.isInStandbyMode())
                .shutdown(metadata.isShutdown())
                .startTime(Optional.ofNullable(metadata.getRunningSince()).map(Date::toString).orElse(null))
                .numJobsExec(metadata.getNumberOfJobsExecuted())
                .jsClass(metadata.getJobStoreClass().getName())
                .jsPersistent(metadata.isJobStoreSupportsPersistence())
                .jsClustered(metadata.isJobStoreClustered())
                .tpClass(metadata.getThreadPoolClass().getName())
                .tpSize(metadata.getThreadPoolSize())
                .build();
    }

    @Override
    public boolean isInStandbyMode() throws SchedulerException {
        return _scheduler.isInStandbyMode();
    }

    @Override
    public void standby() throws SchedulerException {
        _scheduler.standby();
    }

    @Override
    public void pauseAll() throws SchedulerException {
        _scheduler.pauseAll();
    }

    @Override
    public void resumeAll() throws SchedulerException {
        _scheduler.resumeAll();
    }

    @Override
    public Set<KeyEntry> getJobKeys(String groupPrefix) throws SchedulerException {
        return  _scheduler.getJobKeys(groupPrefix == null
                ? GroupMatcher.anyJobGroup()
                : GroupMatcher.jobGroupStartsWith(groupPrefix))
                .stream().map(Utils::keyEntry)
                .collect(Collectors.toSet());
    }

    @Override
    public JobEntry getJobDetail(KeyEntry jobKey) throws SchedulerException {
        JobDetail detail = _scheduler.getJobDetail(jobKey(jobKey));
        return JobEntry.builder()
                .key(keyEntry(detail.getKey()))
                .clazz(detail.getJobClass().getName())
                .jobDataMap(jobDataMap(detail.getJobDataMap()))
                .description(detail.getDescription())
                .durable(detail.isDurable())
                .persist(detail.isPersistJobDataAfterExecution())
                .concurrent(!detail.isConcurrentExectionDisallowed())
                .recoverable(detail.requestsRecovery())
                .build();
    }

    @Override
    public Set<KeyEntry> getTriggerKeys(String groupPrefix) throws SchedulerException {
        return _scheduler.getTriggerKeys(groupPrefix == null
                ? GroupMatcher.anyTriggerGroup()
                : GroupMatcher.triggerGroupStartsWith(groupPrefix))
                .stream().map(Utils::keyEntry)
                .collect(Collectors.toSet());
    }

    private Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }

    @Override
    public TriggerEntry getTrigger(KeyEntry triggerKey) throws SchedulerException {
        Trigger trigger = _scheduler.getTrigger(triggerKey(triggerKey));
        return TriggerEntry.builder()
                .key(keyEntry(trigger.getKey()))
                .job(keyEntry(trigger.getJobKey()))
                .jobDataMap(jobDataMap(trigger.getJobDataMap()))
                .description(trigger.getDescription())
                .calendar(trigger.getCalendarName())
                .priority(trigger.getPriority())
                .mayFireAgain(trigger.mayFireAgain())
                .startTime(toInstant(trigger.getStartTime()))
                .endTime(toInstant(trigger.getEndTime()))
                .nextFireTime(toInstant(trigger.getNextFireTime()))
                .previousFireTime(toInstant(trigger.getPreviousFireTime()))
                .finalFireTime(toInstant(trigger.getFinalFireTime()))
                .misfireInstruction(trigger.getMisfireInstruction())
                .build();
    }

    public void start(ConfigData config, Stack<ShutdownAction> shutdownActions) throws SchedulerException {
        Utils.set(_scheduler, ScriptManagerMBean.KEY, scriptManagerName);
        Utils.set(_scheduler, ScheduleManagerMBean.KEY, scheduleManagerName);
        Utils.set(_scheduler, StreamerMBean.KEY, streamerName);
        Utils.set(_scheduler, ChatManagerMBean.KEY, chatManagerName);
        Utils.set(_scheduler, Config.KEY, config);
        shutdownActions.add(_scheduler::shutdown);
        _scheduler.start();
        triggerIfExists("start");
    }
}

