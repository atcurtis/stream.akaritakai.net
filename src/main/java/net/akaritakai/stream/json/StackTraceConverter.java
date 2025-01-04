package net.akaritakai.stream.json;

import com.fasterxml.jackson.databind.util.StdConverter;
import net.akaritakai.stream.models.telemetry.StackTraceEntry;

public class StackTraceConverter extends StdConverter<StackTraceElement, StackTraceEntry> {

    public static StackTraceConverter INSTANCE = new StackTraceConverter();

    @Override
    public StackTraceEntry convert(StackTraceElement stackTraceElement) {
        StackTraceEntry.StackTraceEntryBuilder builder = StackTraceEntry.builder();
        if (stackTraceElement.isNativeMethod()) {
            if (stackTraceElement.getFileName() != null) {
                builder.fileName(stackTraceElement.getFileName())
                        .lineNumber(stackTraceElement.getLineNumber());
            } else {
                builder.fileName("native");
            }
        } else {
            builder.methodName(stackTraceElement.getMethodName())
                    .fileName(stackTraceElement.getFileName())
                    .lineNumber(stackTraceElement.getLineNumber());
        }
        return builder.build();
    }
}
