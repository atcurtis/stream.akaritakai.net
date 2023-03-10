package net.akaritakai.stream.handler.quartz;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.quartz.request.PauseAllRequest;
import org.apache.commons.lang3.Validate;
import org.quartz.Scheduler;

public class PauseAllHandler extends AbstractHandler<PauseAllRequest> {

    private final Scheduler _scheduler;

    public PauseAllHandler(Scheduler scheduler, CheckAuth checkAuth) {
        super(PauseAllRequest.class, checkAuth);
        _scheduler = scheduler;
    }

    @Override
    protected void validateRequest(PauseAllRequest request) {
        Validate.notNull(request, "request cannot be null");
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, PauseAllRequest listJobsRequest, HttpServerResponse response) {
        try {
            _scheduler.pauseAll();
           handleSuccess("OK", "text/plain", response);
            } catch (Exception e) {
            handleFailure("Error exiting standby", response, e);
        }
    }

}
