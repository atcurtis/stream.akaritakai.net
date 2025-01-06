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
import net.akaritakai.stream.chat.EmojiStore;
import net.akaritakai.stream.chat.FortuneStore;
import net.akaritakai.stream.config.Config;
import net.akaritakai.stream.config.ConfigData;
import net.akaritakai.stream.config.Options;
import net.akaritakai.stream.config.ShutdownAction;
import net.akaritakai.stream.debug.TouchTimer;
import net.akaritakai.stream.handler.*;
import net.akaritakai.stream.handler.info.LogFetchHandler;
import net.akaritakai.stream.handler.info.LogSendHandler;
import net.akaritakai.stream.handler.telemetry.TelemetryFetchHandler;
import net.akaritakai.stream.handler.telemetry.TelemetrySendHandler;
import net.akaritakai.stream.log.DashboardLogAppender;
import net.akaritakai.stream.net.DataUrlStreamHandlerFactory;
import net.akaritakai.stream.scheduling.ScheduleManager;
import net.akaritakai.stream.script.ScriptManager;
import net.akaritakai.stream.streamer.Streamer;
import net.akaritakai.stream.telemetry.TelemetryStore;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.*;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static net.akaritakai.stream.config.GlobalNames.*;

public class Main {

    private static final Logger LOG;
    static {
        DataUrlStreamHandlerFactory.register();

        File cfg = new File("logback.xml");
        if (cfg.exists() && System.getProperty("logback.configurationFile") == null) {
            System.setProperty("logback.configurationFile", cfg.getAbsolutePath());
        }
        LOG = LoggerFactory.getLogger(Main.class);
    }

    private final String version = getClass().getPackage().getImplementationVersion();
    private RouterHelper router;
    private int exitCode = 1;
    private Executor shutdownExecutor = Runnable::run;
    private final Options opt = new Options();

    private enum Command {
        RUN,
        STREAM_START, STREAM_STOP, STREAM_PAUSE, STREAM_RESUME,
        CHAT_CLEAR, CHAT_ENABLE, CHAT_DISABLE, CHAT_WRITE, CHAT_CMD, CHAT_SETEMOJI,
        HEALTH,
        TIME,
    }

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

    private void handleArguments(String[] args) {
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
                .metavar("FILE")
                .type(File.class)
                .action(new LoadConfig());
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
        argumentParser.addArgument("--delay").dest("delay").type(Duration.class).help(FeatureControl.SUPPRESS);
        argumentParser.addArgument("--seekTime").dest("seekTime").type(Duration.class).help(FeatureControl.SUPPRESS);
        argumentParser.addArgument("--startAt").dest("startAt").type(Instant.class).help(FeatureControl.SUPPRESS);
        argumentParser.addArgument("--host").dest("host").setDefault("localhost").help(FeatureControl.SUPPRESS);
        Subparsers subparsers = argumentParser.addSubparsers().metavar("COMMAND");
        Subparser run = subparsers.addParser("run").setDefault("command", Command.RUN)
                .description("Start the server");
        run.addArgument("--sslMedia")
                .dest("sslMedia")
                .type(Boolean.class)
                .setDefault(false)
                .help("Media available on SSL");
        run.addArgument("--emojisFile")
                .dest("emojisFile")
                .help("Custom emojis json file")
                .metavar("FILE");
        Subparsers stream = subparsers.addParser("stream").description("Stream Operations").addSubparsers().dest("subcommand");
        Subparser p = stream.addParser("start").setDefault("command", Command.STREAM_START)
                .description("Start the stream");
        p.addArgument("--delay").dest("delay").type(Duration.class);
        p.addArgument("--seekTime").dest("seekTime").type(Duration.class);
        p.addArgument("--startAt").dest("startAt").type(Instant.class);
        p.addArgument("video-name").dest("video");

        stream.addParser("stop").setDefault("command", Command.STREAM_STOP)
                .description("Stop the stream");
        stream.addParser("pause").setDefault("command", Command.STREAM_PAUSE)
                .description("Pause the stream");
        p = stream.addParser("resume").setDefault("command", Command.STREAM_RESUME)
                .description("Resume the stream");
        p.addArgument("--delay").dest("delay").type(Duration.class);
        p.addArgument("--seekTime").dest("seekTime").type(Duration.class);
        p.addArgument("--startAt").dest("startAt").type(Instant.class);

        Subparsers chat = subparsers.addParser("chat").description("Chat/Interactive Operations").addSubparsers();
        chat.addParser("clear").setDefault("command", Command.CHAT_CLEAR)
                .description("Clear the chat history");
        chat.addParser("enable").setDefault("command", Command.CHAT_ENABLE)
                .description("Enable chat");
        chat.addParser("disable").setDefault("command", Command.CHAT_DISABLE)
                .description("Disable chat");
        p = chat.addParser("write").setDefault("command", Command.CHAT_WRITE);
        p.addArgument("nickname").dest("nickname");
        p.addArgument("TEXT").nargs("+").dest("text");
        p = chat.addParser("cmd").setDefault("command", Command.CHAT_CMD);
        p.addArgument("nickname").dest("nickname");
        p.addArgument("TEXT").nargs("+").dest("text");
        p = chat.addParser("setemoji").setDefault("command", Command.CHAT_SETEMOJI);
        p.addArgument("emoji-name").dest("nickname");
        p.addArgument("emoji-value").dest("emoji");
        subparsers.addParser("health").setDefault("command", Command.HEALTH);
        subparsers.addParser("time").setDefault("command", Command.TIME);
        try {
            argumentParser.parseArgs(args, this.opt);
        } catch (ArgumentParserException e) {
            argumentParser.handleError(e);
            System.exit(e instanceof HelpScreenException ? 0 : 1);
        }
    }

    private static class LoadConfig implements ArgumentAction {
        @Override
        public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag, Object value) throws ArgumentParserException {
            try {
                if (!(value instanceof File file)) {
                    throw new ArgumentParserException("Expected file", parser);
                }
                if (!file.isFile() || !file.canRead()) {
                    LOG.error("File not found or not readable: {}", file);
                    throw new ArgumentParserException("File not found", parser);
                }
                attrs.put(arg.getDest(), Config.getConfig(file.toURI().toURL()));
            } catch (MalformedURLException e) {
                throw new ArgumentParserException(e, parser);
            }
        }

        @Override
        public void onAttach(Argument arg) {
        }

        @Override
        public boolean consumeArgument() {
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        TouchTimer startTimer = new TouchTimer();
        Main main = new Main();
        main.handleArguments(args);
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
            if (main.opt.config == null) {
                main.opt.config = ConfigData.builder().build();
            }
            if (main.opt.apiKey == null) {
                main.opt.apiKey = main.opt.config.getApiKey();
            }
            if (main.opt.port == null) {
                main.opt.port = main.opt.config.getPort();
            }
            if (main.opt.sslPort == null) {
                main.opt.port = main.opt.config.getSslPort();
            }
            if (main.opt.emojisFile == null && main.opt.config.getEmojisFile() != null) {
                main.opt.emojisFile = new File(main.opt.config.getEmojisFile());
            }
            main.select(startTimer, shutdown, shutdownActions);
        } catch (Exception ex) {
            shutdown.completeExceptionally(ex);
        }
    }

    private void select(TouchTimer startTimer,
                        CompletableFuture<Void> shutdown, Stack<ShutdownAction> shutdownActions) throws Exception {
        switch ((Command) opt.command) {
            case RUN -> main0(startTimer, shutdown, shutdownActions);
            default -> {
                String name = opt.command.toString();
                int i = name.indexOf("_");
                if (i < 0) {
                    i = name.length();
                }
                String clsName = name.substring(0, 1) + name.substring(1, i).toLowerCase(Locale.US);
                String methodName = "run";
                if (i < name.length()) {
                    methodName = name.substring(i + 1).toLowerCase();
                }

                Class<?> cls = Class.forName(getClass().getPackageName() + ".admin." + clsName);
                Method method = cls.getMethod(methodName);

                Object rc = method.invoke(cls.getConstructor(TouchTimer.class, Options.class, CompletableFuture.class, Stack.class)
                        .newInstance(startTimer, opt, shutdown, shutdownActions));

                exitCode = rc == null ? 0 : rc instanceof Number num ? num.intValue() : -1;
                shutdown.complete(null);
            }
        }
    }

    private void main0(TouchTimer startTimer,
                      CompletableFuture<Void> shutdown, Stack<ShutdownAction> shutdownActions) throws Exception {
        final ConfigData config = opt.config;

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        Optional<SelfSignedCertificate> ssc = Optional.of(SelfSignedCertificate.create());
        ssc.ifPresent(s -> shutdownActions.add(s::delete));

        ScriptManager scriptManager = new ScriptManager(startTimer);
        mBeanServer.registerMBean(scriptManager, scriptManagerName);

        ScheduleManager scheduleManager = new ScheduleManager(startTimer);
        mBeanServer.registerMBean(scheduleManager, scheduleManagerName);

        CheckAuth auth = new CheckAuthImpl(opt.apiKey);

        Vertx vertx = Vertx.vertx().exceptionHandler(ex -> {
            LOG.error("Exception", ex);
        });
        shutdownExecutor = task -> vertx.executeBlocking(Executors.callable(task));
        shutdownActions.add(() -> vertx.close().toCompletionStage().toCompletableFuture().join());

        TelemetryStore telemetryStore = new TelemetryStore();
        mBeanServer.registerMBean(telemetryStore, telemetryStoreName);

        Streamer streamer = new Streamer(vertx, config);
        mBeanServer.registerMBean(streamer, streamerName);
        startTimer.touch("streamer created");

        EmojiStore emojiStore = new EmojiStore();
        FortuneStore fortuneStore = new FortuneStore();

        for (String f : Optional.ofNullable(config.getFortuneFiles()).map(Arrays::asList)
                .orElse(Collections.emptyList())) {
            fortuneStore.addFile(new File(f));
        }

        ChatManager chatManager = new ChatManager(startTimer, emojiStore, fortuneStore, config);
        mBeanServer.registerMBean(chatManager, chatManagerName);
        startTimer.touch("Chat manager created");

        chatManager.addCustomEmojis(opt.emojisFile);
        startTimer.touch("after loading custom emojis");

        /* Now to setup the vertx router */
        router = new RouterHelper(vertx, opt.isSslApi());

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

        registerPostApiHandler("/telemetry/fetch", new TelemetryFetchHandler(telemetryStore, auth));
        registerGetHandler("/telemetry", new TelemetrySendHandler(telemetryStore));

        registerPostApiHandler("/log/fetch", new LogFetchHandler(vertx, auth));
        registerGetHandler("/log", new LogSendHandler());

        registerGetHandler("/health", new HealthCheckHandler());
        registerGetHandler("/time", new TimeHandler());

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
                    LOG.info("sslMedia = {}", opt.isSslMedia());
                    router.sslRouter.route("/media/*").handler(opt.isSslMedia() ? handler : new RedirectHandler(() -> httpPort[0]));
                });
            } else {
                LOG.warn("Media directory not found or not readable: {}", config.getMediaRootDir());
            }
        }


        startTimer.touch("SetupScheduler");
        scheduleManager.setup();

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
                .listen(opt.port, bind, event -> {
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
                .listen(opt.sslPort, bind, event -> {
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
