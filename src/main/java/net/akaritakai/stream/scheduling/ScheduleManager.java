package net.akaritakai.stream.scheduling;

import io.vertx.core.Vertx;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.InitDB;
import net.akaritakai.stream.config.Config;
import net.akaritakai.stream.config.ConfigData;
import net.akaritakai.stream.config.ShutdownAction;
import net.akaritakai.stream.debug.TouchTimer;
import net.akaritakai.stream.handler.RouterHelper;
import net.akaritakai.stream.handler.quartz.*;
import net.akaritakai.stream.scheduling.jobs.*;
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
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

public class ScheduleManager extends NotificationBroadcasterSupport implements ScheduleManagerMBean {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduleManager.class);

    private final Scheduler _scheduler;

    public ScheduleManager(TouchTimer timer, ConfigData config, Stack<ShutdownAction> shutdownActions) throws Exception {
        _scheduler = createFactory().getScheduler();
        Utils.set(_scheduler, Config.KEY, config);
        shutdownActions.add(_scheduler::shutdown);
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
        schedulerProperties.setProperty("org.quartz.dataSource.myDS.URL", "jdbc:sqlite:scheduler.db");
        schedulerProperties.setProperty("org.quartz.scheduler.jmx.export", "true");
        schedulerProperties.setProperty("org.quartz.scheduler.jmx.objectName", "net.akaritakai.stream:type=Scheduler");

        try (Connection connection = DriverManager.getConnection(schedulerProperties.getProperty("org.quartz.dataSource.myDS.URL"));
             ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM QRTZ_LOCKS")) {
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
    public void triggerIfExists(String message, String chat, JobDataMap jobDataMap) {
        Utils.triggerIfExists(_scheduler, message, chat, jobDataMap);
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
    public SchedulerMetaData getMetaData() throws SchedulerException {
        return _scheduler.getMetaData();
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
    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> jobKeyGroupMatcher) throws SchedulerException {
        return _scheduler.getJobKeys(jobKeyGroupMatcher);
    }

    @Override
    public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
        return _scheduler.getJobDetail(jobKey);
    }

    @Override
    public Set<? extends TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> triggerKeyGroupMatcher) throws SchedulerException {
        return _scheduler.getTriggerKeys(triggerKeyGroupMatcher);
    }

    @Override
    public Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException {
        return _scheduler.getTrigger(triggerKey);
    }
}

