package net.akaritakai.stream.handler.quartz;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.models.quartz.request.StatusRequest;
import net.akaritakai.stream.scheduling.ScheduleManagerMBean;
import net.akaritakai.stream.scheduling.Utils;
import org.apache.commons.lang3.Validate;

import static net.akaritakai.stream.config.GlobalNames.scheduleManagerName;

public class StatusHandler extends AbstractBlockingHandler<StatusRequest> {

    public StatusHandler(Vertx vertx, CheckAuth checkAuth) {
        super(StatusRequest.class, vertx, checkAuth);
    }

    @Override
    protected void validateRequest(StatusRequest request) {
        Validate.notNull(request, "request cannot be null");
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, StatusRequest listJobsRequest, HttpServerResponse response) {
        executeBlocking(() -> Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class).getMetaData())
                .map(this::writeValue)
                .onSuccess(metadata -> {
                    handleSuccess(metadata, "application/json", response);
                })
                .onFailure(ex -> handleFailure("Error retrieving metadata", response, ex));
    }
}
