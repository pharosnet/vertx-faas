package org.pharosnet.vertx.faas.database.core.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.sqlclient.Transaction;
import org.pharosnet.vertx.faas.database.api.SqlContext;
import org.pharosnet.vertx.faas.database.core.CachedTransaction;
import org.pharosnet.vertx.faas.database.core.Databases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AbstractDatabaseService {

    private static final Logger log = LoggerFactory.getLogger(AbstractDatabaseService.class);

    private static final String host_key = "_database_hosts";

    public AbstractDatabaseService(Vertx vertx) {
        this.vertx = vertx;
        this.databases = Databases.get(vertx);
    }

    private final Vertx vertx;
    private final Databases databases;

    public Vertx vertx() {
        return vertx;
    }

    public Databases databases() {
        return databases;
    }

    protected Boolean isLocalTransaction(String hostId) {
        return hostId.equals(this.databases.getHostId());
    }

    protected Future<Optional<String>> getTransactionHostId(SqlContext context) {
        Promise<Optional<String>> promise = Promise.promise();
        this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> map.get(context.getId()))
                .onSuccess(r -> {
                    if (log.isDebugEnabled()) {
                        log.error("get transaction hostId({}) by context({})", r, context.getId());
                    }
                    if (r == null) {
                        promise.complete(Optional.empty());
                        return;
                    }
                    if (r instanceof String) {
                        String hostId = (String) r;
                        promise.complete(Optional.of(hostId));
                        return;
                    }
                    promise.fail(String.format("get transaction hostId(%s) by context(%s) failed, that value is not String type", r.toString(), context.getId()));
                })
                .onFailure(e -> {
                    log.error("get transaction hostId by context({}) failed", context.getId());
                    promise.complete(Optional.empty());
                });
        return promise.future();
    }

    protected Future<Void> putHostedTransaction(SqlContext context, CachedTransaction transaction) {
        return this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> map.put(context.getId(), this.databases.getHostId(), this.databases.getTransactionCachedTTL()))
                .compose(r -> this.databases().putCachedTransaction(context.getId(), transaction));
    }

    protected Future<Void> releaseTransaction(SqlContext context) {
        return this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> {
                    map.remove(context.getId());
                    return this.databases().removeCachedTransaction(context.getId());
                });
    }

    protected void releaseTransactionWithRollback(SqlContext context) {
        this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> {
                    map.remove(context.getId());
                    return this.databases().getCachedTransaction(context.getId());
                })
                .compose(cachedTransactionOptional -> {
                    if (cachedTransactionOptional.isEmpty()) {
                        return Future.succeededFuture();
                    }
                    CachedTransaction cachedTransaction = cachedTransactionOptional.get();
                    Transaction transaction = cachedTransaction.getTransaction();
                    if (transaction != null) {
                        return cachedTransaction.getTransaction().rollback()
                                .eventually(v -> cachedTransaction.getConnection().close());
                    }
                    return Future.succeededFuture();
                })
                .compose(v -> this.databases().removeCachedTransaction(context.getId()))
                .onFailure(e -> {
                    if (log.isWarnEnabled()) {
                        log.warn("release cached transaction failed, context id = {}", context.getId(), e);
                    }
                })
                .onSuccess(v -> {
                    if (log.isDebugEnabled()) {
                        log.warn("release cached transaction succeed, context id = {}", context.getId());
                    }
                });
    }

    protected Future<Optional<CachedTransaction>> getTransaction(SqlContext context) {
        return this.databases().getCachedTransaction(context.getId());
    }

    private Future<Optional<Record>> fetchHostRecord(String hostId) {
        Promise<Optional<Record>> promise = Promise.promise();
        ServiceDiscovery discovery = this.databases.getDiscovery();
        if (discovery == null) {
            return Future.failedFuture("database is not in distribute mode");
        }
        discovery.getRecord(r -> r.getRegistration().equals(hostId))
                .onSuccess(record -> {
                    if (record == null) {
                        promise.complete(Optional.empty());
                        return;
                    }
                    promise.complete(Optional.of(record));
                })
                .onFailure(e -> {
                    log.error("get host({}) record failed", hostId);
                    promise.complete(Optional.empty());
                });
        return promise.future();
    }

    protected Future<ServiceReference> getRemoteServiceReference(String hostId) {
        ServiceDiscovery discovery = this.databases.getDiscovery();
        if (discovery == null) {
            return Future.failedFuture("database is not in distribute mode");
        }

        Promise<ServiceReference> promise = Promise.promise();

        this.fetchHostRecord(hostId)
                .onSuccess(record -> {
                    if (record.isEmpty()) {
                        log.error("database get remote service failed, not found host({}).", hostId);
                        promise.fail(String.format("get remote(%s) database service failed, not found.", hostId));
                        return;
                    }
                    promise.complete(discovery.getReference(record.get()));
                })
                .onFailure(e -> {
                    log.error("database get remote service failed, error occurs with host({}).", hostId, e);
                    promise.fail(new Exception(String.format("get remote(%s) database service failed", hostId), e));
                });
        return promise.future();
    }

}
