package net.akaritakai.stream.script;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.akaritakai.stream.chat.ChatManagerMBean;
import net.akaritakai.stream.chat.FortuneStoreMBean;
import net.akaritakai.stream.debug.TouchTimer;
import net.akaritakai.stream.json.ExceptionConverter;
import net.akaritakai.stream.models.quartz.TaskEntry;
import net.akaritakai.stream.models.quartz.TaskResult;
import net.akaritakai.stream.scheduling.ScheduleManagerMBean;
import net.akaritakai.stream.scheduling.StateStoreMBean;
import net.akaritakai.stream.scheduling.Utils;
import net.akaritakai.stream.streamer.StreamerMBean;
import net.akaritakai.stream.telemetry.TelemetryStoreMBean;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import javax.script.*;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static net.akaritakai.stream.config.GlobalNames.*;

public class ScriptManager implements ScriptManagerMBean {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptManager.class);
    private static final Pattern CRLF_TRIM = Pattern.compile("^[\r\n]*(.*?)[\r\n]*$");

    private final ScriptEngine _scriptEngine;

    public ScriptManager(TouchTimer timer) {
        this(new ScriptEngineManager().getEngineByMimeType("application/javascript"));
        LOG.info("ScriptEngine: {}", _scriptEngine);

        Bindings bindings = _scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("polyglot.js.allowHostAccess", true);
        bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) s -> {
            LOG.warn("ScriptEngine class lookup: {}", s);
            return true;
        });
        timer.touch("ScriptEngine: {}", _scriptEngine);
    }

    public ScriptManager(ScriptEngine scriptEngine) {
        _scriptEngine = scriptEngine;
    }

    private Logger getLogger(TaskEntry task) {
        return LoggerFactory.getLogger(getClass().getPackageName()
                + "." + (task.getGroup() != null ? task.getGroup() : "none")
                + "." + (task.getName() != null ? task.getName() : "NoName"));
    }

    private static CharSequence trimCrLf(CharSequence source) {
        Matcher m = CRLF_TRIM.matcher(source);
        return m.matches() ? source.subSequence(m.start(1), m.end(1)) : source;
    }


    @Override
    public TaskResult executeTask(TaskEntry task, Map<String, Object> jobData) {
        Objects.requireNonNull(task);
        TaskResult.TaskResultBuilder resultBuilder = TaskResult.builder()
                .name(task.getName());
        if (task.getGroup() != null) {
            resultBuilder.group(task.getGroup());
        }
        Logger log = getLogger(task);
        AtomicInteger logSequence = new AtomicInteger();

        ScriptContext context = new SimpleScriptContext();
        context.setBindings(_scriptEngine.createBindings(), ScriptContext.ENGINE_SCOPE);
        context.setReader(Reader.nullReader());
        context.setErrorWriter(new StringWriter() {
            @Override
            public void flush() {
                super.flush();
                synchronized (getBuffer()) {
                    log.warn("[{}] {}", String.format("%04d", logSequence.incrementAndGet()),
                            trimCrLf(getBuffer().toString()));
                    getBuffer().setLength(0);
                }
            }
        });
        StringWriter stdout = new StringWriter() {
            private int position;
            @Override
            public void flush() {
                super.flush();
                synchronized (getBuffer()) {
                    log.info("[{}] {}", String.format("%04d", logSequence.incrementAndGet()),
                            trimCrLf(getBuffer().subSequence(position, getBuffer().length())));
                    position = getBuffer().length();
                }
            }
        };
        context.setWriter(stdout);

        Bindings scope = context.getBindings(ScriptContext.ENGINE_SCOPE);

        scope.put("polyglot.js.allowHostAccess", true);
        scope.put("polyglot.js.allowHostClassLookup", new Predicate<String>() {
            @Override
            public boolean test(String className) {
                LOG.info("Class Lookup: {}", className);
                return true;
            }
        });

        scope.put("log", log);
        scope.put("job", jobData);

        expose(scope, "chat", chatManagerName, ChatManagerMBean.class);
        expose(scope, "streamer", streamerName, StreamerMBean.class);
        expose(scope, "scriptManager", scriptManagerName, ScriptManagerMBean.class);
        expose(scope, "scheduleManager", scheduleManagerName, ScheduleManagerMBean.class);
        expose(scope, "telemetry", telemetryStoreName, TelemetryStoreMBean.class);
        expose(scope, "state", stateStoreName, StateStoreMBean.class);
        expose(scope, "fortune", fortuneStoreName, FortuneStoreMBean.class);

        try {
            resultBuilder.success(false);
            Object result = _scriptEngine.eval(new StringReader(task.getCode()), context);
            resultBuilder.success(true);

            if (result != null) {
                resultBuilder.outputClassName(result.getClass().getName());
                if (result instanceof CharSequence || result instanceof Number) {
                    resultBuilder.output(result.toString());
                } else {
                    try {
                        resultBuilder.output(new ObjectMapper().writeValueAsString(result));
                    } catch (JsonMappingException e) {
                        log.warn("serialization", e);
                        resultBuilder.output(result.toString());
                    }
                }
            }
        } catch (Throwable e) {
            log.debug("Exception", e);
            resultBuilder.exception(ExceptionConverter.INSTANCE.convert(e));
        } finally {
            resultBuilder.stdout(stdout.toString());
        }
        return resultBuilder.build();
    }

    private void expose(Bindings scope, String name, ObjectName objectName, Class<?> beanClass) {
        assert beanClass.isInterface() && beanClass.getSimpleName().endsWith("MBean");

        scope.put(name, Utils.beanProxy(objectName, beanClass));

        HashSet<Class<?>> seen = new HashSet<>();

        for (Method m : beanClass.getMethods()) {
            exposeForMethod(scope, m, seen);
        }
    }

    private void exposeType(Bindings scope, Class<?> type, HashSet<Class<?>> seen) {
        if (type == null || !seen.add(type)) {
            return;
        }

        if (type.getPackageName().startsWith("net.akaritakai.stream.models.")
                && Stream.of(type.getMethods()).anyMatch(m -> Modifier.isStatic(m.getModifiers()))) {
            LOG.debug("Exposing {}", type.getName());
            scope.put(type.getSimpleName(), type);
        }

        for (Class<?> cls : type.getClasses()) {
            exposeType(scope, cls.componentType(), seen);
            if (!type.isPrimitive() && !scope.containsKey(cls.getSimpleName())) {
                for (Method m : cls.getMethods()) {
                    exposeForMethod(scope, m, seen);
                }
            }
        }
    }

    private void exposeForMethod(Bindings scope, Method m, HashSet<Class<?>> seen) {
        exposeType(scope, m.getReturnType(), seen);
        for (Class<?> type : m.getParameterTypes()) {
            exposeType(scope, type, seen);
        }
    }

    private Connection getConnection() throws SQLException {
        return DBConnectionManager.getInstance().getConnection("myDS");
    }

    @Override
    public TaskEntry loadTask(String name, String group) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT SCRIPT_NAME,SCRIPT_GROUP,SCRIPT_TEXT" +
                     " FROM SCRIPT_STORE WHERE SCRIPT_NAME=? AND SCRIPT_GROUP=?")) {
            stmt.setString(1, name);
            stmt.setString(2, group);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return TaskEntry.builder()
                            .name(rs.getString(1))
                            .group(rs.getString(2))
                            .code(rs.getString(3))
                            .build();
                }
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public boolean saveTask(TaskEntry task) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("REPLACE INTO SCRIPT_STORE" +
                     " (SCRIPT_NAME, SCRIPT_GROUP, SCRIPT_TEXT) VALUES (?,?,?)")) {
            stmt.setString(1, task.getName());
            stmt.setString(2, task.getGroup());
            stmt.setString(3, task.getCode());
            return stmt.executeUpdate() == 1;
        }
    }

    @Override
    public Set<String> getAllTaskGroups() throws SQLException {
        HashSet<String> groupNames = new HashSet<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT SCRIPT_GROUP FROM SCRIPT_STORE");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                groupNames.add(rs.getString(1));
            }
        }
        return groupNames;
    }

    @Override
    public Set<String> getAllTaskNames(String group) throws SQLException {
        HashSet<String> groupNames = new HashSet<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT SCRIPT_GROUP FROM SCRIPT_STORE WHERE SCRIPT_GROUP=?")) {
            stmt.setString(1, group);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupNames.add(rs.getString(1));
                }
            }
        }
        return groupNames;
    }

}
