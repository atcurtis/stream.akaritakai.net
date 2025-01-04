package net.akaritakai.stream.log;

import ch.qos.logback.core.OutputStreamAppender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DashboardLogAppender<E> extends OutputStreamAppender<E> {

    /**
     * Immediate flush means that the underlying writer or output stream will be flushed at the end of each append
     * operation. Immediate flush is slower but ensures that each append request is actually written. If
     * <code>immediateFlush</code> is set to {@code false}, then there is a good chance that the last few logs events
     * are not actually written to persistent media if and when the application crashes.
     */
    private boolean _immediateFlush = true;

    public DashboardLogAppender() {
    }

    @Override
    protected void append(E event) {
        try {
            synchronized (getGlobalOutputStream()) {
                super.append(event);
            }
        } catch (Throwable ex) {
           addError("Unable to write to stream for appender " + getName() + " event: " + event, ex);
        }
    }

    public interface DashboardLogListener {
        void acceptLog(byte[] data);
    }

    public static class DashboardOutputStream extends ByteArrayOutputStream {

        private final Set<DashboardLogListener> _listeners = ConcurrentHashMap.newKeySet();

        @Override
        public void flush() throws IOException {
            byte[] bytes = flush0();
            _listeners.forEach(listener -> listener.acceptLog(bytes));
        }

        private synchronized byte[] flush0() throws IOException {
            byte[] bytes = toByteArray();
            reset();
            return bytes;
        }

        public void addListener(DashboardLogListener listener) {
            _listeners.add(listener);
        }
    }

    private static DashboardOutputStream OUTPUT_STREAM;

    public static synchronized DashboardOutputStream getGlobalOutputStream() {
        if (OUTPUT_STREAM == null) {
            OUTPUT_STREAM = new DashboardOutputStream();
        }
        return OUTPUT_STREAM;
    }
}
