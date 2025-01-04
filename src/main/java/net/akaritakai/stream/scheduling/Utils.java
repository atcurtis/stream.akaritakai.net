package net.akaritakai.stream.scheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.akaritakai.stream.models.quartz.KeyEntry;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final WeakHashMap<Scheduler, ConcurrentMap<SchedulerAttribute<?>, AtomicReference<?>>> TRANSIENT
             = new WeakHashMap<>();

    private static <T> AtomicReference<T> getReference(Scheduler scheduler, SchedulerAttribute<T> key) {
        ConcurrentMap<SchedulerAttribute<T>, AtomicReference<T>> concurrentMap;
        synchronized (TRANSIENT) {
            @SuppressWarnings({"UnnecessaryLocalVariable", "rawtypes"}) ConcurrentMap map
                    = TRANSIENT.computeIfAbsent(scheduler, ignore -> new ConcurrentHashMap<>());
            //noinspection unchecked
            concurrentMap = map;
        }
        return concurrentMap.computeIfAbsent(key, ignore -> new AtomicReference<>());
    }

    public static <T> T get(Scheduler scheduler, SchedulerAttribute<T> key) {
        return getReference(scheduler, key).get();
    }

    public static <T> T set(Scheduler scheduler, SchedulerAttribute<T> key, T value) {
        return getReference(scheduler, key).getAndSet(value);
    }

    public static void triggerIfExists(Scheduler scheduler, String name) {
        triggerIfExists(scheduler, JobKey.jobKey(name));
    }
    public static void triggerIfExists(Scheduler scheduler, String name, String group) {
        triggerIfExists(scheduler, JobKey.jobKey(name, group));
    }
    public static void triggerIfExists(Scheduler scheduler, JobKey jobKey) {
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.triggerJob(jobKey);
            }
        } catch (SchedulerException e) {
            LOG.warn("Failed to trigger", e);
        }
    }

    public static void triggerIfExists(Scheduler scheduler, String name, JobDataMap jobDataMap) {
        triggerIfExists(scheduler, JobKey.jobKey(name), jobDataMap);
    }

    public static void triggerIfExists(Scheduler scheduler, String name, String group, JobDataMap jobDataMap) {
        triggerIfExists(scheduler, JobKey.jobKey(name, group), jobDataMap);
    }

    public static void triggerIfExists(Scheduler scheduler, JobKey jobKey, JobDataMap jobDataMap) {
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.triggerJob(jobKey, jobDataMap);
            }
        } catch (SchedulerException e) {
            LOG.warn("Failed to trigger", e);
        }
    }

    public static <T> T beanProxy(ObjectName objectName, Class<T> mxBean) {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        return JMX.newMBeanProxy(mBeanServer, objectName, mxBean);
    }

    public static String writeAsString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new CompletionException(e);
        }
    }

    public static JobKey jobKey(KeyEntry entry) {
        String group = entry.getGroup();
        return group != null
                ? new JobKey(entry.getName(), group)
                : new JobKey(entry.getName());
    }

    public static TriggerKey triggerKey(KeyEntry entry) {
        String group = entry.getGroup();
        return group != null
                ? new TriggerKey(entry.getName(), group)
                : new TriggerKey(entry.getName());
    }

    public static KeyEntry keyEntry(JobKey key) {
        KeyEntry.KeyEntryBuilder builder = KeyEntry.builder().name(key.getName());
        String group = key.getGroup();
        return (group != null ? builder.group(group) : builder).build();
    }

    public static KeyEntry keyEntry(TriggerKey key) {
        KeyEntry.KeyEntryBuilder builder = KeyEntry.builder().name(key.getName());
        String group = key.getGroup();
        return (group != null ? builder.group(group) : builder).build();
    }

    public static String stringify(Object o) {
        if (!(o instanceof CharSequence || o instanceof Number)) {
            try {
                return OBJECT_MAPPER.writeValueAsString(o);
            } catch (JsonProcessingException e) {
                LOG.debug("Couldn't json", e);
            }
        }
        return o != null ? o.toString() : null;
    }

    public static Map<String, String> jobDataMap(JobDataMap map) {
        Map<String, String> jobDataMap = new HashMap<>();
        map.forEach((key, value) -> jobDataMap.put(key, stringify(value)));
        return jobDataMap;
    }

    public static Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }
}
