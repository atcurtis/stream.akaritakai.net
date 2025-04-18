package net.akaritakai.stream.telemetry;

import net.akaritakai.stream.models.telemetry.TelemetryEvent;

import java.util.Collection;

public interface TelemetryStoreMBean {
    Collection<TelemetryEvent> getTelemetry();
    long size();
}
