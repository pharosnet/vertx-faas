package org.pharosnet.vertx.faas.database.api;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDALImpl<R, Id> implements AbstractDAL<R, Id> {

    private static final Logger log = LoggerFactory.getLogger(AbstractDALImpl.class);

    public AbstractDALImpl(Vertx vertx) {
        this.vertx = vertx;
        this.service = DatabaseService.proxy(vertx);
    }

    private final Vertx vertx;
    private final DatabaseService service;

    protected Vertx vertx() {
        return vertx;
    }
    protected DatabaseService service() { return service; }

    @Override
    public Future<Void> begin(SqlContext context) {
        Promise<Void> promise = Promise.promise();
        this.service.begin(context, r -> {
            if (r.failed()) {
                log.error("database begin transaction failed");
                promise.fail(r.cause());
                return;
            }
            promise.complete();
        });
        return promise.future();
    }

    @Override
    public Future<Void> commit(SqlContext context) {
        Promise<Void> promise = Promise.promise();
        this.service.commit(context, r -> {
            if (r.failed()) {
                log.error("database commit transaction failed");
                promise.fail(r.cause());
                return;
            }
            promise.complete();
        });
        return promise.future();
    }

    @Override
    public Future<Void> rollback(SqlContext context) {
        Promise<Void> promise = Promise.promise();
        this.service.rollback(context, r -> {
            if (r.failed()) {
                log.error("database rollback transaction failed");
                promise.fail(r.cause());
                return;
            }
            promise.complete();
        });
        return promise.future();
    }

    @Override
    public Future<QueryResult> query(SqlContext context, QueryArg arg) {
        Promise<QueryResult> promise = Promise.promise();
        this.service.query(context, arg, r -> {
            if (r.failed()) {
                log.error("database query failed, arg = {}", arg.toJson().encode());
                promise.fail(r.cause());
                return;
            }
            promise.complete(r.result());
        });
        return promise.future();
    }

}
