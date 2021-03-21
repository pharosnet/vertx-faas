package org.pharosnet.vertx.faas;

import io.vertx.core.Vertx;

public abstract class AbstractFn {

    public AbstractFn(Vertx vertx) {
        this.vertx = vertx;
    }

    private final Vertx vertx;

    protected Vertx vertx() {
        return vertx;
    }

}