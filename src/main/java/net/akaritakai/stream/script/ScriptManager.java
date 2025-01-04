package net.akaritakai.stream.script;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.akaritakai.stream.chat.ChatManagerMBean;
import net.akaritakai.stream.debug.TouchTimer;
import net.akaritakai.stream.json.ExceptionConverter;
import net.akaritakai.stream.models.quartz.TaskEntry;
import net.akaritakai.stream.models.quartz.TaskResult;
import net.akaritakai.stream.scheduling.Utils;
import net.akaritakai.stream.streamer.StreamerMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import javax.script.*;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static net.akaritakai.stream.config.GlobalNames.*;

public class ScriptManager implements ScriptManagerMBean {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptManager.class);

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


    @Override
    public TaskResult executeTask(TaskEntry task) {
        Objects.requireNonNull(task);
        TaskResult.TaskResultBuilder resultBuilder = TaskResult.builder()
                .name(task.getName());
        if (task.getGroup() != null) {
            resultBuilder.group(task.getGroup());
        }

        ScriptContext context = new SimpleScriptContext();
        context.setBindings(_scriptEngine.createBindings(), ScriptContext.ENGINE_SCOPE);
        Bindings scope = context.getBindings(ScriptContext.ENGINE_SCOPE);

        scope.put("polyglot.js.allowHostAccess", true);
        scope.put("polyglot.js.allowHostClassLookup", new Predicate<String>() {
            @Override
            public boolean test(String className) {
                LOG.info("Class Lookup: {}", className);
                return true;
            }
        });

        scope.put("log", LOG);

        //scope.put("scheduler", Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class));
        expose(scope, "chat", chatManagerName, ChatManagerMBean.class);
        expose(scope, "streamer", streamerName, StreamerMBean.class);

        try {
            resultBuilder.success(false);
            Object result = _scriptEngine.eval(new StringReader(task.getCode()), context);
            resultBuilder.success(true);

            if (result != null) {
                if (result instanceof CharSequence || result instanceof Number) {
                    resultBuilder.output(result.toString());
                } else {
                    try {
                        resultBuilder.output(new ObjectMapper().writeValueAsString(result));
                    } catch (JsonMappingException e) {
                        LOG.warn("serialization", e);
                        resultBuilder.output(result.toString());
                    }
                }
            }

        } catch (Throwable e) {
            resultBuilder.exception(ExceptionConverter.INSTANCE.convert(e));
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
            LOG.info("Exposing {}", type.getName());
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


}
