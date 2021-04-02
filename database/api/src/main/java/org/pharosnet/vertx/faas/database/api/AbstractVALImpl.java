package org.pharosnet.vertx.faas.database.api;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractVALImpl implements AbstractVAL {

    private static final Logger log = LoggerFactory.getLogger(AbstractVALImpl.class);

    public AbstractVALImpl(Vertx vertx) {
        this.vertx = vertx;
        this.service = DatabaseService.proxy(vertx);
    }

    private final Vertx vertx;
    private final DatabaseService service;

    protected Vertx vertx() {
        return vertx;
    }
    protected DatabaseService service() { return service; }

}
