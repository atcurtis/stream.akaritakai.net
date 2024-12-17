package net.akaritakai.stream.handler.quartz;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.quartz.request.StandbyRequest;
import org.apache.commons.lang3.Validate;
import org.quartz.Scheduler;

public class StandbyHandler extends AbstractHandler<StandbyRequest> {

    private final Scheduler _scheduler;

    public StandbyHandler(Scheduler scheduler, CheckAuth checkAuth) {
        super(StandbyRequest.class, checkAuth);
        _scheduler = scheduler;
    }

    @Override
    protected void validateRequest(StandbyRequest request) {
        Validate.notNull(request, "request cannot be null");
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, StandbyRequest listJobsRequest, HttpServerResponse response) {
        try {
            if (_scheduler.isInStandbyMode()) {
                handleFailure("Already in standby mode", response, new IllegalStateException());
                return;
            }
            _scheduler.standby();
            handleSuccess("OK", "text/plain", response);
            } catch (Exception e) {
            handleFailure("Error setting standby", response, e);
        }
    }
}
