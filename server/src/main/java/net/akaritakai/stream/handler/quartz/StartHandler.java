package net.akaritakai.stream.handler.quartz;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.quartz.request.ListJobsRequest;
import org.apache.commons.lang3.Validate;
import org.quartz.Scheduler;

public class StartHandler extends AbstractHandler<ListJobsRequest> {

    private final Scheduler _scheduler;

    public StartHandler(Scheduler scheduler, CheckAuth checkAuth) {
        super(ListJobsRequest.class, checkAuth);
        _scheduler = scheduler;
    }

    @Override
    protected void validateRequest(ListJobsRequest request) {
        Validate.notNull(request, "request cannot be null");
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, ListJobsRequest listJobsRequest, HttpServerResponse response) {
        try {
            if (!_scheduler.isInStandbyMode()) {
                handleFailure("Not in standby mode", response, new IllegalStateException());
                return;
            }
            _scheduler.startDelayed(1);
            handleSuccess("OK", "text/plain", response);
            } catch (Exception e) {
            handleFailure("Error exiting standby", response, e);
        }
    }

}
