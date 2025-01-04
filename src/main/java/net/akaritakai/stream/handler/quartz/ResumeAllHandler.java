package net.akaritakai.stream.handler.quartz;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.quartz.request.ResumeAllRequest;
import net.akaritakai.stream.scheduling.ScheduleManagerMBean;
import net.akaritakai.stream.scheduling.Utils;
import org.apache.commons.lang3.Validate;
import org.quartz.Scheduler;

import static net.akaritakai.stream.config.GlobalNames.scheduleManagerName;

public class ResumeAllHandler extends AbstractBlockingHandler<ResumeAllRequest> {

    public ResumeAllHandler(Vertx vertx, CheckAuth checkAuth) {
        super(ResumeAllRequest.class, vertx, checkAuth);
    }

    @Override
    protected void validateRequest(ResumeAllRequest request) {
        Validate.notNull(request, "request cannot be null");
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, ResumeAllRequest listJobsRequest, HttpServerResponse response) {
        executeBlocking(() -> {
            Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class).resumeAll();
            return null;
        })
                .onSuccess(unused -> handleSuccess("OK", "text/plain", response))
                .onFailure(ex -> handleFailure("Error exiting standby", response, ex));
    }

}
