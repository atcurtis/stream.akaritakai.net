package net.akaritakai.stream.json;

import com.fasterxml.jackson.databind.util.StdConverter;
import net.akaritakai.stream.models.telemetry.ExceptionEvent;
import net.akaritakai.stream.models.telemetry.StackTraceEntry;

import javax.script.ScriptException;
import java.util.ArrayList;

public class ExceptionConverter extends StdConverter<Throwable, ExceptionEvent> {

    public static final ExceptionConverter INSTANCE = new ExceptionConverter();

    @Override
    public ExceptionEvent convert(Throwable ex) {
        ExceptionEvent.ExceptionEventBuilder builder = ExceptionEvent.builder();
        builder.message(ex.getMessage());
        if (ex.getCause() != null && ex.getCause() != ex) {
            builder.cause(convert(ex.getCause()));
        }
        ArrayList<StackTraceEntry> entries = new ArrayList<>();
        if (ex instanceof ScriptException se) {
            entries.add(StackTraceEntry.builder()
                            .fileName(se.getFileName())
                            .lineNumber(se.getLineNumber())
                            .columnNumber(se.getColumnNumber())
                    .build());
         }
        StackTraceElement[] elements = ex.getStackTrace();
        if (elements != null && elements.length > 0) {
            for (StackTraceElement element : elements) {
                entries.add(StackTraceConverter.INSTANCE.convert(element));
            }
        }
        if (!entries.isEmpty()) {
            builder.stackTrace(entries.toArray(new StackTraceEntry[0]));
        }
        return builder.build();
    }
}
