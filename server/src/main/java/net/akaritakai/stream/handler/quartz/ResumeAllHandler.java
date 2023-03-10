package net.akaritakai.stream.handler.quartz;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.quartz.request.ResumeAllRequest;
import org.apache.commons.lang3.Validate;
import org.quartz.Scheduler;

public class ResumeAllHandler extends AbstractHandler<ResumeAllRequest> {

    private final Scheduler _scheduler;

    public ResumeAllHandler(Scheduler scheduler, CheckAuth checkAuth) {
        super(ResumeAllRequest.class, checkAuth);
        _scheduler = scheduler;
    }

    @Override
    protected void validateRequest(ResumeAllRequest request) {
        Validate.notNull(request, "request cannot be null");
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, ResumeAllRequest listJobsRequest, HttpServerResponse response) {
        try {
            _scheduler.resumeAll();
           handleSuccess("OK", "text/plain", response);
            } catch (Exception e) {
            handleFailure("Error exiting standby", response, e);
        }
    }

}
