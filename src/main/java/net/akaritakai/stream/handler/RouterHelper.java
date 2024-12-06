package net.akaritakai.stream.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class RouterHelper {
    public final Router router;
    public final Router sslRouter;
    public final boolean sslApi;

    public RouterHelper(Vertx vertx, boolean sslApi) {
        router = Router.router(vertx);
        sslRouter = Router.router(vertx);
        this.sslApi = sslApi;
    }

    public void registerGetHandler(String uri, Handler<RoutingContext> handler) {
        router.get(uri).handler(handler);
        sslRouter.get(uri).handler(handler);
    }

    public void registerPostApiHandler(String uri, Handler<RoutingContext> handler) {
        if (!sslApi) {
            router.post(uri).handler(BodyHandler.create()).handler(handler);
        }
        sslRouter.post(uri).handler(BodyHandler.create()).handler(handler);
    }

    public void failureHandler(Handler<RoutingContext> ctx, Handler<RoutingContext> handler) {
        router.route().handler(ctx).failureHandler(handler);
        sslRouter.route().handler(ctx).failureHandler(handler);
    }

    public void handler(Handler<RoutingContext> handler) {
        router.route().handler(handler);
        sslRouter.route().handler(handler);
    }
}
