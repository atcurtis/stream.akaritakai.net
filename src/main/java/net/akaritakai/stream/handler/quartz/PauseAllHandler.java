package net.akaritakai.stream.handler.quartz;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.models.quartz.request.PauseAllRequest;
import net.akaritakai.stream.scheduling.ScheduleManagerMBean;
import net.akaritakai.stream.scheduling.Utils;
import org.apache.commons.lang3.Validate;

import static net.akaritakai.stream.config.GlobalNames.scheduleManagerName;

public class PauseAllHandler extends AbstractBlockingHandler<PauseAllRequest> {

    public PauseAllHandler(Vertx vertx, CheckAuth checkAuth) {
        super(PauseAllRequest.class, vertx, checkAuth);
    }

    @Override
    protected void validateRequest(PauseAllRequest request) {
        Validate.notNull(request, "request cannot be null");
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, PauseAllRequest listJobsRequest, HttpServerResponse response) {
        executeBlocking(() -> {
            Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class).pauseAll();
            return null;
        })
                .onSuccess(unused -> handleSuccess("OK", "text/plain", response))
                .onFailure(ex -> handleFailure("Error exiting standby", response, ex));
    }

}
