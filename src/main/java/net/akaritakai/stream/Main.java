package net.akaritakai.stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import net.akaritakai.stream.chat.ChatManager;
import net.akaritakai.stream.config.Config;
import net.akaritakai.stream.config.ConfigData;
import net.akaritakai.stream.config.ShutdownAction;
import net.akaritakai.stream.debug.TouchTimer;
import net.akaritakai.stream.handler.*;
import net.akaritakai.stream.handler.info.LogFetchHandler;
import net.akaritakai.stream.handler.info.LogSendHandler;
import net.akaritakai.stream.handler.telemetry.TelemetryFetchHandler;
import net.akaritakai.stream.handler.telemetry.TelemetrySendHandler;
import net.akaritakai.stream.log.DashboardLogAppender;
import net.akaritakai.stream.scheduling.ScheduleManager;
import net.akaritakai.stream.script.ScriptManager;
import net.akaritakai.stream.streamer.Streamer;
import net.akaritakai.stream.telemetry.TelemetryStore;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.*;

import static net.akaritakai.stream.config.GlobalNames.*;

public class Main {

    private static final Logger LOG;
    static {
        File cfg = new File("logback.xml");
        if (cfg.exists() && System.getProperty("logback.configurationFile") == null) {
            System.setProperty("logback.configurationFile", cfg.getAbsolutePath());
        }
        LOG = LoggerFactory.getLogger(Main.class);
    }

    private final String version = getClass().getPackage().getImplementationVersion();
    private RouterHelper router;
    private boolean sslApi;
    private boolean sslMedia;
    private int exitCode = 1;
    private Executor shutdownExecutor = Runnable::run;


    private Main() {
        LoggerContext logCtx = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
        logEncoder.setContext(logCtx);
        logEncoder.setPattern("%-12date{YYYY-MM-dd HH:mm:ss.SSS} %-5level - %msg%n");
        logEncoder.start();

        ConsoleAppender<ILoggingEvent> logConsoleAppender = new ConsoleAppender<>();
        logConsoleAppender.setContext(logCtx);
        logConsoleAppender.setName("console");
        logConsoleAppender.setEncoder(logEncoder);
        logConsoleAppender.start();

        logEncoder = new PatternLayoutEncoder();
        logEncoder.setContext(logCtx);
        logEncoder.setPattern("%-12date{YYYY-MM-dd HH:mm:ss.SSS} %-5level - %msg%n");
        logEncoder.start();

        DashboardLogAppender<ILoggingEvent> dashboardLogAppender = new DashboardLogAppender<>();
        dashboardLogAppender.setContext(logCtx);
        dashboardLogAppender.setName("dashboard");
        dashboardLogAppender.setEncoder(logEncoder);
        dashboardLogAppender.setImmediateFlush(true);
        dashboardLogAppender.setOutputStream(DashboardLogAppender.getGlobalOutputStream());
        dashboardLogAppender.start();

        ch.qos.logback.classic.Logger log = logCtx.getLogger(ROOT_LOGGER_NAME);
        log.setAdditive(true);
        log.setLevel(Level.INFO);
        //log.addAppender(logConsoleAppender);
        log.addAppender(dashboardLogAppender);
    }

    private Namespace handleArguments(String[] args) {
        ArgumentParser argumentParser = ArgumentParsers.newFor("StreamServer")
                .addHelp(true)
                .terminalWidthDetection(true)
                .singleMetavar(true)
                .build()
                .defaultHelp(true)
                .description("A basic video sharing and streaming service")
                .version(version);
        argumentParser.addArgument("--version")
                .help("Print build version")
                .action(Arguments.version());
        argumentParser.addArgument("-f", "--configFile")
                .dest("configFile")
                .help("Configuration file")
                .metavar("FILE");
        argumentParser.addArgument("-p", "--port")
                .dest("port")
                .help("Service TCP port number")
                .type(Integer.class)
                .metavar("PORT");
        argumentParser.addArgument("--sslPort")
                .dest("sslPort")
                .help("Service TCP port number")
                .type(Integer.class)
                .setDefault(8443)
                .metavar("PORT");
        argumentParser.addArgument("--apiKey")
                .dest("apiKey")
                .help("API Configuration Key")
                .metavar("KEY");
        argumentParser.addArgument("--sslApi")
                .dest("sslApi")
                .type(Boolean.class)
                .help("API Configuration requires SSL");
        argumentParser.addArgument("--sslMedia")
                .dest("sslMedia")
                .type(Boolean.class)
                .setDefault(false)
                .help("Media available on SSL");
        argumentParser.addArgument("--emojisFile")
                .dest("emojisFile")
                .help("Custom emojis json file")
                .metavar("FILE");
        try {
            return argumentParser.parseArgs(args);
        } catch (ArgumentParserException e) {
            argumentParser.handleError(e);
            System.exit(e instanceof HelpScreenException ? 0 : 1);
            return null;
        }
    }

    private ConfigData loadConfig(Namespace ns) {
        return Config.getConfig(Optional
                .ofNullable(ns.getString("configFile"))
                .map(File::new)
                .filter(file -> {
                    if (file.isFile() && file.canRead()) {
                        return true;
                    } else {
                        LOG.error("File not found or not readable: {}", file);
                        return false;
                    }
                }).map(file -> {
                    try {
                        return file.toURI().toURL();
                    } catch (MalformedURLException e) {
                        LOG.error("Unexpected exception", e);
                        throw new RuntimeException(e);
                    }
                }).orElse(null));
    }

    public static void main(String[] args) throws Exception {
        TouchTimer startTimer = new TouchTimer();
        Main main = new Main();
        Namespace ns = main.handleArguments(args);
        CompletableFuture<Void> shutdown = new CompletableFuture<>();
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown.complete(null);
            try {
                shutdownLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));
        Stack<ShutdownAction> shutdownActions = new Stack<>();
        shutdown.whenComplete((unused, throwable) -> {
            LOG.info("Shutdown initiated");
            main.shutdownExecutor.execute(() -> {
                if (throwable != null) {
                    LOG.error("Unhandled fsilure, {}", startTimer, throwable);
                } else {
                    LOG.info("Shutdown, {}", startTimer);
                }
                while (!shutdownActions.isEmpty()) {
                    ShutdownAction action = shutdownActions.pop();
                    try {
                        LOG.info("Shutdown action {}", action);
                        action.call();
                    } catch (Exception ex) {
                        LOG.error("Exception from shutdown handler {}", action, ex);
                    }
                }
                shutdownLatch.countDown();
                System.exit(main.exitCode);
            });
        });
        try {
            main.main0(startTimer, ns, shutdown, shutdownActions);
        } catch (Exception ex) {
            shutdown.completeExceptionally(ex);
        }
    }

    private void main0(TouchTimer startTimer, Namespace ns,
                      CompletableFuture<Void> shutdown, Stack<ShutdownAction> shutdownActions) throws Exception {

        sslApi = Optional.ofNullable(ns.getBoolean("sslApi")).orElse(false);
        sslMedia = Optional.ofNullable(ns.getBoolean("sslMedia")).orElse(false);

        ConfigData config = loadConfig(ns);
        LOG.info("Loaded config {}", config);
        startTimer.touch("Loaded config");

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        Optional<SelfSignedCertificate> ssc = Optional.of(SelfSignedCertificate.create());
        ssc.ifPresent(s -> shutdownActions.add(s::delete));

        ScriptManager scriptManager = new ScriptManager(startTimer);
        mBeanServer.registerMBean(scriptManager, scriptManagerName);

        ScheduleManager scheduleManager = new ScheduleManager(startTimer);
        mBeanServer.registerMBean(scheduleManager, scheduleManagerName);

        CheckAuth auth = new CheckAuthImpl(
                Optional.ofNullable(ns.getString("apiKey")).orElse(config.getApiKey()));

        Vertx vertx = Vertx.vertx().exceptionHandler(ex -> {
            LOG.error("Exception", ex);
        });
        shutdownExecutor = task -> vertx.executeBlocking(Executors.callable(task));
        shutdownActions.add(() -> vertx.close().toCompletionStage().toCompletableFuture().join());

        TelemetryStore telemetryStore = new TelemetryStore();

        Streamer streamer = new Streamer(vertx, config);
        mBeanServer.registerMBean(streamer, streamerName);
        startTimer.touch("streamer created");

        ChatManager chatManager = new ChatManager(startTimer, config);
        mBeanServer.registerMBean(chatManager, chatManagerName);
        startTimer.touch("Chat manager created");

        chatManager.addCustomEmojis(Optional.ofNullable(ns.getString("emojisFile")).map(File::new)
                        .orElse(Optional.ofNullable(config.getEmojisFile()).map(File::new).orElse(null)));
        startTimer.touch("after loading custom emojis");

        /* Now to setup the vertx router */
        router = new RouterHelper(vertx, sslApi);

        Optional.of((Handler<RoutingContext>) event -> {
            TouchTimer.of(event).ifPresent(touchTimer -> touchTimer.touch("failureHandler"));
            if (event.failure() instanceof ClosedChannelException) {
                LOG.debug("Closed Channel, {}", TouchTimer.of(event).orElse(null));
            } else if (!event.response().headWritten()) {
                event.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                        .setStatusCode(500)
                        .end(Json.encode(event.failure()));
            } else {
                LOG.error("Unhandled exception in router, {}", TouchTimer.of(event).orElse(null), event.failure());
            }
        }).ifPresent(handler -> {
            Handler<RoutingContext> ctx = event -> {
                event.data().put("TouchTimer", new TouchTimer().touch("uri={}", event.request().uri()));
                event.next();
            };
            router.failureHandler(ctx, handler);
        });

        if (config.isLogRequestInfo()) {
            Optional.of((Handler<RoutingContext>) event -> {
                HttpServerRequest request = event.request();
                LOG.info("Got request: method={}, uri={}, headers={}",
                        request.method(),
                        request.absoluteURI(),
                        request.headers());
                event.next();
            }).ifPresent(router::handler);
        }

        registerGetHandler("/health", new HealthCheckHandler());
        registerGetHandler("/time", new TimeHandler());

        registerPostApiHandler("/telemetry/fetch", new TelemetryFetchHandler(telemetryStore, auth));
        registerGetHandler("/telemetry", new TelemetrySendHandler(telemetryStore));

        registerPostApiHandler("/log/fetch", new LogFetchHandler(vertx, auth));
        registerGetHandler("/log", new LogSendHandler());

        Streamer.register(vertx, router, auth, streamerName);
        ChatManager.register(vertx, router, auth, chatManagerName);
        ScheduleManager.register(vertx, router, auth);

        final int[] httpPort = new int[2];

        if (config.isDevelopment()) {
            Optional.of((Handler<RoutingContext>) event -> {
                event.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                event.next();
            }).ifPresent(router::handler);
        }

        Optional.of(new StaticResourceHandler(vertx.getOrCreateContext())
                        .setWebRoot(config.getWebRootDir())
                        .setCachingEnabled(true)
                        .setCacheEntryTimeout(TimeUnit.HOURS.toMillis(2))
                        .setMaxAgeSeconds(TimeUnit.HOURS.toSeconds(2))
                        .setMaxCacheSize(32 * 1024 * 1024)
                        .setSendVaryHeader(true)
                        .setDirectoryListing(false)
                        .setIncludeHidden(false)
                        .skipCompressionForSuffixes(new HashSet<>(Arrays.asList("jpg", "png", "woff2")))
                        .setAlwaysAsyncFS(true)
                        .setFilesReadOnly(true)
                        .setEnableFSTuning(true)
                        .setEnableRangeSupport(true))
                .ifPresent(router::handler);

        if (config.getMediaRootDir() != null && !config.getMediaRootDir().isEmpty()) {
            File dir = new File(config.getMediaRootDir());
            if (dir.isDirectory() && dir.canRead()) {
                Optional.of(StaticHandler.create(FileSystemAccess.ROOT, config.getMediaRootDir())
                        .setCachingEnabled(true)
                        .setCacheEntryTimeout(TimeUnit.SECONDS.toMillis(10))
                        .setMaxAgeSeconds(2) // let us use 2 second segments
                        .setMaxCacheSize(32 * 1024 * 1024)
                        .setDirectoryListing(false)
                        .setIncludeHidden(false)
                        .skipCompressionForSuffixes(new HashSet<>(Arrays.asList("ts", "mp4", "m3u8")))
                        .setAlwaysAsyncFS(true)
                        .setEnableFSTuning(true)
                        .setEnableRangeSupport(true)).ifPresent(handler -> {
                    router.router.route("/media/*").handler(handler);
                    LOG.info("sslMedia = {}", sslMedia);
                    router.sslRouter.route("/media/*").handler(sslMedia ? handler : new RedirectHandler(() -> httpPort[0]));
                });
            } else {
                LOG.warn("Media directory not found or not readable: {}", config.getMediaRootDir());
            }
        }


        startTimer.touch("SetupScheduler");
        ScheduleManager.setup(scheduleManager.scheduler());

        Promise<Void> startHttp = Promise.promise();
        Promise<Void> startHttps = Promise.promise();

        HttpServerOptions httpServerOptions = new HttpServerOptions();
        HttpServerOptions httpsServerOptions = new HttpServerOptions();

        ssc.ifPresent(selfSignedCertificate -> {
            httpsServerOptions.setSsl(true);
            //noinspection deprecation
            httpsServerOptions.setPemKeyCertOptions(selfSignedCertificate.keyCertOptions());
            httpsServerOptions.setTrustOptions(selfSignedCertificate.trustOptions());
        });

        String bind = Optional.ofNullable(config.getBind())
                .filter(value -> !value.isBlank())
                .orElse("0.0.0.0");

        startTimer.touch("createHttpServer");
        HttpServer httpServer = vertx.createHttpServer(httpServerOptions)
                .requestHandler(router.router)
                .listen(Optional.ofNullable(ns.getInt("port")).orElse(config.getPort()), bind, event -> {
                    if (event.succeeded()) {
                        startTimer.touch("http:listen done, port {}", event.result().actualPort());
                        LOG.info("Started the http server on port {}", event.result().actualPort());
                        httpPort[0] = event.result().actualPort();
                        startHttp.complete();
                    } else {
                        startTimer.touch("http:listen failed, {}", event.cause().getMessage());
                        LOG.error("http:listen failed", event.cause());
                        startHttp.fail(event.cause());
                    }
                });
        shutdownActions.add(() -> httpServer.close().toCompletionStage().toCompletableFuture().join());

        startTimer.touch("createHttpsServer");
        HttpServer httpsServer = vertx.createHttpServer(httpsServerOptions)
                .requestHandler(router.sslRouter)
                .listen(Optional.ofNullable(ns.getInt("sslPort")).orElse(config.getSslPort()), bind, event -> {
                    if (event.succeeded()) {
                        startTimer.touch("https:listen done, port {}", event.result().actualPort());
                        LOG.info("Started the https server on port {}", event.result().actualPort());
                        httpPort[1] = event.result().actualPort();
                        startHttps.complete();
                    } else {
                        startTimer.touch("https:listen failed, {}", event.cause().getMessage());
                        LOG.error("https:listen failed", event.cause());
                        startHttps.fail(event.cause());
                    }
                });
        shutdownActions.add(() -> httpsServer.close().toCompletionStage().toCompletableFuture().join());

        Future.all(startHttp.future(), startHttps.future())
                .onSuccess(event -> {
                    startTimer.touch("Ports bound");
                    try {
                        scheduleManager.start(config, shutdownActions);
                    } catch (SchedulerException e) {
                        shutdown.completeExceptionally(e);
                        return;
                    }
                    startTimer.touch("Startup completed");
                    LOG.info("Trace {}", startTimer);
                    exitCode = 0;
                })
                .onFailure(shutdown::completeExceptionally);
    }

    private void registerGetHandler(String uri, Handler<RoutingContext> handler) {
        router.registerGetHandler(uri, handler);
    }

    private void registerPostApiHandler(String uri, Handler<RoutingContext> handler) {
        router.registerPostApiHandler(uri, handler);
    }
}
