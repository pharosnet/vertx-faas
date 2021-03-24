package org.pharosnet.vertx.faas.component.http.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public abstract class InterceptorHandler implements Handler<RoutingContext> {

    public InterceptorHandler(Vertx vertx) {
        this.vertx = vertx;
    }

    private final Vertx vertx;

    protected Vertx vertx() {
        return vertx;
    }

    @Override
    public abstract void handle(RoutingContext event);

}
