package org.pharosnet.vertx.faas.database.core.impl;

import io.vertx.core.*;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.sqlclient.*;
import org.pharosnet.vertx.faas.database.api.*;
import org.pharosnet.vertx.faas.database.core.CachedTransaction;
import org.pharosnet.vertx.faas.database.core.commons.Latency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DatabaseServiceImpl extends AbstractDatabaseService implements DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);

    public static MessageConsumer<JsonObject> register(Vertx vertx) {
        return new ServiceBinder(vertx).setAddress(DatabaseService.SERVICE_ADDRESS).register(DatabaseService.class, new DatabaseServiceImpl(vertx));
    }

    protected DatabaseServiceImpl(Vertx vertx) {
        super(vertx);
    }

    @Override
    public void begin(SqlContext context, Handler<AsyncResult<TransactionResult>> handler) {
        if (!this.databases().isServiceApplied()) {
            handler.handle(ServiceException.fail(503, "database service is not applied"));
            return;
        }
        Latency latency = new Latency();
        latency.start();
        this.getTransactionHostId(context)
                .compose(hostId -> {
                    if (hostId.isPresent()) {
                        return Future.failedFuture(String.format("database service can not begin a transaction with context(%s), it has be begun.", context.getId()));
                    }
                    return Future.succeededFuture();
                })
                .compose(_r -> this.databases().getNode()
                        .getConnection()
                        .compose(connection -> {
                            Promise<CachedTransaction> cachedTransactionPromise = Promise.promise();
                            connection.begin()
                                    .onSuccess(transaction -> cachedTransactionPromise.complete(new CachedTransaction(connection, transaction)))
                                    .onFailure(e -> {
                                        log.error("transaction begin failed", e);
                                        cachedTransactionPromise.fail(e);
                                    });
                            return cachedTransactionPromise.future();
                        }))
                .compose(ct -> this.putHostedTransaction(context, ct))
                .onSuccess(v -> {
                    TransactionResult result = new TransactionResult();
                    result.setLatency(latency.end().value());
                    handler.handle(Future.succeededFuture(result));
                    if (log.isDebugEnabled()) {
                        log.debug("database service begin transaction succeed, latency = {}, context = {}", latency.toString(), context.getId());
                    }
                })
                .onFailure(e -> {
                    log.error("database service begin transaction failed with context({})", context.getId(), e);
                    handler.handle(ServiceException.fail(
                            500,
                            "database service begin transaction failed.",
                            new JsonObject()
                                    .put("contextId", context.getId())
                                    .put("host", this.databases().getHostId())
                                    .put("cause", e)
                    ));
                });
    }

    @Override
    public void commit(SqlContext context, Handler<AsyncResult<TransactionResult>> handler) {
        if (!this.databases().isServiceApplied()) {
            handler.handle(ServiceException.fail(503, "database service is not applied"));
            return;
        }
        this.getTransactionHostId(context)
                .compose(hostId -> {
                    if (hostId.isEmpty()) {
                        return Future.failedFuture(String.format("database service can not commit a transaction with context(%s), it has not be begun.", context.getId()));
                    }
                    return Future.succeededFuture(hostId.get());
                })
                .compose(hostId -> {
                    Promise<TransactionResult> resultPromise = Promise.promise();
                    if (this.isLocalTransaction(hostId)) {
                        this.executeCommitOrRollback(context, true, resultPromise);
                    } else {
                        this.getRemoteServiceReference(hostId)
                                .compose(serviceReference -> {
                                    DatabaseService target = serviceReference.getAs(DatabaseService.class);
                                    Promise<TransactionResult> remotePromise = Promise.promise();
                                    target.commit(context, remotePromise);
                                    return remotePromise.future();
                                })
                                .onSuccess(result -> {
                                    if (log.isDebugEnabled()) {
                                        log.debug("database service commit transaction succeed by remote({})", hostId);
                                    }
                                    resultPromise.complete(result);
                                })
                                .onFailure(e -> {
                                    log.error("database service commit transaction failed, context = {}, remote host = {}", context.getId(), hostId, e);
                                    resultPromise.fail(new Exception(String.format("database service commit transaction failed by remote %s", hostId), e));
                                });
                    }
                    return resultPromise.future();
                })
                .onSuccess(result -> {
                    handler.handle(Future.succeededFuture(result));
                    if (log.isDebugEnabled()) {
                        log.debug("database service commit transaction succeed, latency = {}ms, context = {}", result.getLatency(), context.getId());
                    }
                })
                .onFailure(e -> {
                    log.error("database service commit transaction failed with context({})", context.getId(), e);
                    handler.handle(ServiceException.fail(
                            500,
                            "database service commit transaction failed.",
                            new JsonObject()
                                    .put("contextId", context.getId())
                                    .put("host", this.databases().getHostId())
                                    .put("cause", e)
                    ));
                });
    }

    @Override
    public void rollback(SqlContext context, Handler<AsyncResult<TransactionResult>> handler) {
        if (!this.databases().isServiceApplied()) {
            handler.handle(ServiceException.fail(503, "database service is not applied"));
            return;
        }
        this.getTransactionHostId(context)
                .compose(hostId -> {
                    if (hostId.isEmpty()) {
                        return Future.failedFuture(String.format("database service can not rollback a transaction with context(%s), it has not be begun.", context.getId()));
                    }
                    return Future.succeededFuture(hostId.get());
                })
                .compose(hostId -> {
                    Promise<TransactionResult> resultPromise = Promise.promise();
                    if (this.isLocalTransaction(hostId)) {
                        this.executeCommitOrRollback(context, false, resultPromise);
                    } else {
                        this.getRemoteServiceReference(hostId)
                                .compose(serviceReference -> {
                                    DatabaseService target = serviceReference.getAs(DatabaseService.class);
                                    Promise<TransactionResult> remotePromise = Promise.promise();
                                    target.commit(context, remotePromise);
                                    return remotePromise.future();
                                })
                                .onSuccess(result -> {
                                    if (log.isDebugEnabled()) {
                                        log.debug("database service rollback transaction succeed by remote({})", hostId);
                                    }
                                    resultPromise.complete(result);
                                })
                                .onFailure(e -> {
                                    log.error("database service rollback transaction failed, context = {}, remote host = {}", context.getId(), hostId, e);
                                    resultPromise.fail(new Exception(String.format("database service rollback transaction failed by remote %s", hostId), e));
                                });
                    }
                    return resultPromise.future();
                })
                .onSuccess(result -> {
                    handler.handle(Future.succeededFuture(result));
                    if (log.isDebugEnabled()) {
                        log.debug("database service rollback transaction succeed, latency = {}ms, context = {}", result.getLatency(), context.getId());
                    }
                })
                .onFailure(e -> {
                    log.error("database service rollback transaction failed with context({})", context.getId(), e);
                    handler.handle(ServiceException.fail(
                            500,
                            "database service rollback transaction failed.",
                            new JsonObject()
                                    .put("contextId", context.getId())
                                    .put("host", this.databases().getHostId())
                                    .put("cause", e)
                    ));
                });
    }

    private void executeCommitOrRollback(SqlContext context, boolean commit, Handler<AsyncResult<TransactionResult>> handler) {
        Latency latency = new Latency();
        latency.start();
        this.getTransaction(context)
                .compose(cachedTransaction -> {
                    if (cachedTransaction.isEmpty()) {
                        return Future.failedFuture("transaction %s is lost");
                    }
                    SqlConnection connection = cachedTransaction.get().getConnection();
                    Transaction transaction = cachedTransaction.get().getTransaction();
                    if (commit) {
                        return transaction.commit()
                                .eventually(v -> connection.close());
                    }
                    return transaction.rollback()
                            .eventually(v -> connection.close());
                })
                .compose(r -> this.releaseTransaction(context))
                .onSuccess(r -> {
                    TransactionResult result = new TransactionResult();
                    result.setLatency(latency.end().value());
                    handler.handle(Future.succeededFuture(result));
                })
                .onFailure(e -> {
                    if (commit) {
                        log.error("database service commit transaction failed, context = {}, host = {}", context.getId(), this.databases().getHostId(), e);
                        handler.handle(Future.failedFuture(new Exception("database service commit transaction failed", e)));
                    } else {
                        log.error("database service rollback transaction failed, context = {}, host = {}", context.getId(), this.databases().getHostId(), e);
                        handler.handle(Future.failedFuture(new Exception("database service rollback transaction failed", e)));
                    }
                });
    }

    @Override
    public void query(SqlContext context, QueryArg arg, Handler<AsyncResult<QueryResult>> handler) {
        if (!this.databases().isServiceApplied()) {
            handler.handle(ServiceException.fail(503, "database service is not applied"));
            return;
        }
        String sql = Optional.ofNullable(arg.getQuery()).orElse("").trim();
        if (sql.isBlank()) {
            handler.handle(ServiceException.fail(400, "query is empty"));
            return;
        }
        this.getTransactionHostId(context)
                .compose(hostId -> {
                    Promise<QueryResult> resultPromise = Promise.promise();
                    if (hostId.isEmpty()) {
                        // no transaction
                        Pool pool;
                        if (Optional.ofNullable(arg.getSlaverMode()).orElse(false)) {
                            pool = this.databases().getSlaver();
                            if (pool == null) {
                                resultPromise.fail("database service query failed, no slaver mode");
                                return resultPromise.future();
                            }
                        } else {
                            pool = this.databases().getNode();
                        }
                        PreparedQuery<RowSet<Row>> preparedQuery = pool.preparedQuery(sql);
                        this.executeQuery(context, preparedQuery, arg, resultPromise);
                    } else {
                        if (this.isLocalTransaction(hostId.get())) {
                            // local transaction
                            this.getTransaction(context)
                                    .compose(cachedTransaction -> {
                                        if (cachedTransaction.isEmpty()) {
                                            return Future.failedFuture("transaction %s is lost");
                                        }
                                        SqlConnection connection = cachedTransaction.get().getConnection();
                                        return Future.succeededFuture(connection);

                                    })
                                    .compose(connection -> {
                                        Promise<QueryResult> txPromise = Promise.promise();
                                        PreparedQuery<RowSet<Row>> preparedQuery = connection.preparedQuery(sql);
                                        this.executeQuery(context, preparedQuery, arg, txPromise);
                                        return txPromise.future();
                                    })
                                    .onSuccess(resultPromise::complete)
                                    .onFailure(e -> {
                                        log.error("database service rollback transaction failed, context = {}, host = {}", context.getId(), this.databases().getHostId(), e);
                                        this.releaseTransactionWithRollback(context);
                                        resultPromise.fail(new Exception("database service query failed", e));
                                    });
                        } else {
                            // remote transaction
                            this.getRemoteServiceReference(hostId.get())
                                    .compose(serviceReference -> {
                                        DatabaseService target = serviceReference.getAs(DatabaseService.class);
                                        Promise<QueryResult> remotePromise = Promise.promise();
                                        target.query(context, arg, remotePromise);
                                        return remotePromise.future();
                                    })
                                    .onSuccess(result -> {
                                        if (log.isDebugEnabled()) {
                                            log.debug("database service query succeed by remote({})", hostId);
                                        }
                                        resultPromise.complete(result);
                                    })
                                    .onFailure(e -> {
                                        log.error("database service query failed by remote({}), context = {}", hostId, context.getId(), e);
                                        resultPromise.fail(new Exception(String.format("database service query failed by remote %s", hostId), e));
                                    });
                        }
                    }
                    return resultPromise.future();
                })
                .onSuccess(result -> {
                    handler.handle(Future.succeededFuture(result));
                    if (log.isDebugEnabled()) {
                        log.debug("database service query succeed, latency = {}ms, context = {}, arg = \n{}", result.getLatency(), context.getId(), arg);
                    }
                })
                .onFailure(e -> {
                    log.error("database service query failed with context({}), arg = {}", context.getId(), arg.toJson().encode(), e);
                    handler.handle(ServiceException.fail(
                            500,
                            "database service query failed.",
                            new JsonObject()
                                    .put("contextId", context.getId())
                                    .put("host", this.databases().getHostId())
                                    .put("cause", e)
                    ));
                });
    }

    @SuppressWarnings("unchecked")
    private void executeQuery(SqlContext context, PreparedQuery<RowSet<Row>> preparedQuery, QueryArg arg, Handler<AsyncResult<QueryResult>> handler) {
        Latency latency = new Latency();
        latency.start();
        JsonArray args = arg.getArgs();
        boolean batch = Optional.ofNullable(arg.getBatch()).orElse(false);
        boolean needLastInsertedId = Optional.ofNullable(arg.getNeedLastInsertedId()).orElse(false);
        Future<RowSet<Row>> queryFuture;
        if (args != null && !args.isEmpty()) {
            if (batch) {
                List<Tuple> tuples = new ArrayList<>();
                for (int i = 0; i < args.size(); i++) {
                    JsonArray line = args.getJsonArray(i);
                    tuples.add(Tuple.from(line.getList()));
                }
                queryFuture = preparedQuery.executeBatch(tuples);
            } else {
                Tuple tuple = Tuple.from(args.getList());
                queryFuture = preparedQuery.execute(tuple);
            }
        } else {
            queryFuture = preparedQuery.execute();
        }
        queryFuture.onFailure(e -> {
            log.error("database service query in local server failed, arg = {}", arg.getArgs().encode(), e);
            handler.handle(Future.failedFuture("database service query in local server failed"));
        }).onSuccess(rows -> {

            QueryResult result = new QueryResult();
            if (needLastInsertedId) {
                try {
                    long lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID);
                    result.setLastInsertedId(lastInsertId);
                } catch (Exception exception) {
                    log.error("database service query in local server failed, get last inserted id failed, arg = {}", arg.getArgs().encode(), exception);
                    handler.handle(Future.failedFuture("database service query in local server failed, get last inserted id failed."));
                    return;
                }
            }
            result.setAffected(rows.rowCount());
            List<JsonObject> array = new ArrayList<>();
            for (Row row : rows) {
                array.add(row.toJson());
            }
            result.setRows(array);
            result.setLatency(latency.end().value());
            handler.handle(Future.succeededFuture(result));
            if (log.isDebugEnabled()) {
                log.debug("database service query succeed, latency = {}ms, context = {}, arg = \n{}", result.getLatency(), context.getId(), arg);
            }
        });
    }

}
