package net.akaritakai.stream.handler.quartz;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.models.quartz.request.StandbyRequest;
import net.akaritakai.stream.scheduling.ScheduleManagerMBean;
import net.akaritakai.stream.scheduling.Utils;
import org.apache.commons.lang3.Validate;

import static net.akaritakai.stream.config.GlobalNames.scheduleManagerName;

public class StandbyHandler extends AbstractBlockingHandler<StandbyRequest> {

    public StandbyHandler(Vertx vertx, CheckAuth checkAuth) {
        super(StandbyRequest.class, vertx, checkAuth);
    }

    @Override
    protected void validateRequest(StandbyRequest request) {
        Validate.notNull(request, "request cannot be null");
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, StandbyRequest listJobsRequest, HttpServerResponse response) {
        executeBlocking(() -> {
            ScheduleManagerMBean manager = Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class);
            if (manager.isInStandbyMode()) {
                throw new IllegalStateException("Already in standby mode");
            }
            manager.standby();
            handleSuccess("OK", "text/plain", response);
            return null;
        })
                .onSuccess(unused -> handleSuccess("OK", "text/plain", response))
                .onFailure(ex -> handleFailure("Cannot switch to stand-by", response, ex));
    }
}
