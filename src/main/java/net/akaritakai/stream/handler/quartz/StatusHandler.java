package net.akaritakai.stream.handler.quartz;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.quartz.request.StatusRequest;
import net.akaritakai.stream.models.quartz.response.StatusResponse;
import org.apache.commons.lang3.Validate;
import org.quartz.Scheduler;
import org.quartz.SchedulerMetaData;

import java.util.Date;
import java.util.Optional;

public class StatusHandler extends AbstractHandler<StatusRequest> {

    private final Scheduler _scheduler;

    public StatusHandler(Scheduler scheduler, CheckAuth checkAuth) {
        super(StatusRequest.class, checkAuth);
        _scheduler = scheduler;
    }

    @Override
    protected void validateRequest(StatusRequest request) {
        Validate.notNull(request, "request cannot be null");
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, StatusRequest listJobsRequest, HttpServerResponse response) {
        try {
            SchedulerMetaData metadata = _scheduler.getMetaData();
            handleSuccess(OBJECT_MAPPER.writeValueAsString(StatusResponse.builder()
                    .schedName(metadata.getSchedulerName())
                    .schedInst(metadata.getSchedulerInstanceId())
                    .schedClass(metadata.getSchedulerClass().getName())
                    .isRemote(metadata.isSchedulerRemote())
                    .started(metadata.isStarted())
                    .isInStandbyMode(metadata.isInStandbyMode())
                    .shutdown(metadata.isShutdown())
                    .startTime(Optional.ofNullable(metadata.getRunningSince()).map(Date::toString).orElse(null))
                    .numJobsExec(metadata.getNumberOfJobsExecuted())
                    .jsClass(metadata.getJobStoreClass().getName())
                    .jsPersistent(metadata.isJobStoreSupportsPersistence())
                    .jsClustered(metadata.isJobStoreClustered())
                    .tpClass(metadata.getThreadPoolClass().getName())
                    .tpSize(metadata.getThreadPoolSize())
                    .build()), "application/json", response);
            } catch (Exception e) {
            handleFailure("Error retrieving metadata", response, e);
        }
    }
}
