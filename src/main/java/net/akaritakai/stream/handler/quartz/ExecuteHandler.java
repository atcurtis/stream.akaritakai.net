package net.akaritakai.stream.handler.quartz;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.quartz.commands.ExecuteRequest;
import net.akaritakai.stream.models.quartz.response.ExecuteResponse;
import net.akaritakai.stream.script.ScriptManagerMBean;
import net.akaritakai.stream.scheduling.Utils;
import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.concurrent.Callable;

import static net.akaritakai.stream.config.GlobalNames.scriptManagerName;

public class ExecuteHandler extends AbstractHandler<ExecuteRequest> {

    private final Vertx vertx;

    public ExecuteHandler(Vertx vertx, CheckAuth checkAuth) {
        super(ExecuteRequest.class, checkAuth);
        this.vertx = vertx;
    }

    protected <T> Future<T> executeBlocking(Callable<T> fn) {
        return vertx.executeBlocking(fn);
    }

    @Override
    protected void validateRequest(ExecuteRequest request) {
        Validate.notNull(request, "request cannot be null");
        Validate.notNull(request.getKey(), "request key cannot be null");
        LOG.debug("Execute request: {}", request);
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, ExecuteRequest request, HttpServerResponse response) {
        vertx
                .executeBlocking(() -> OBJECT_MAPPER
                        .writeValueAsString(ExecuteResponse.builder()
                                .result(Utils.beanProxy(scriptManagerName, ScriptManagerMBean.class)
                                        .executeTask(request.getTask(), Collections.emptyMap()))
                                .build()))
                .onSuccess(result -> handleSuccess(result, "text/json", response))
                .onFailure(ex -> handleFailure("Failure executing script", response, ex));
    }
}
