package net.akaritakai.stream.handler;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.akaritakai.stream.CheckAuth;

import java.util.concurrent.Callable;

public abstract class AbstractBlockingHandler<REQUEST> extends AbstractHandler<REQUEST> {

    protected final Vertx _vertx;

    protected AbstractBlockingHandler(Class<REQUEST> requestClass, Vertx vertx, CheckAuth checkAuth) {
        super(requestClass, checkAuth);
        _vertx = vertx;
    }

    protected <T> Future<T> executeBlocking(Callable<T> callable) {
        return _vertx.executeBlocking(callable);
    }
}
