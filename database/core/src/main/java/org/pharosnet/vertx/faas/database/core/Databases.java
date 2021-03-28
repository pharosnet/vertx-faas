package org.pharosnet.vertx.faas.database.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.sqlclient.Pool;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.pharosnet.vertx.faas.database.api.DatabaseService;
import org.pharosnet.vertx.faas.database.core.config.DatabaseConfig;
import org.pharosnet.vertx.faas.database.core.config.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class Databases {

    private static final Logger log = LoggerFactory.getLogger(Databases.class);

    private static final String context_key = "_databases";

    public static Databases get(Vertx vertx) {
        return vertx.getOrCreateContext().get(context_key);
    }

    public Databases(Vertx vertx, DatabaseConfig config) {
        String kind = Optional.ofNullable(config.getClusterKind()).orElse("none").trim();
        if (kind.equalsIgnoreCase("none")) {
            this.distributed = false;
            this.mastered = false;
        } else if (kind.equalsIgnoreCase("master_slaver")) {
            this.distributed = true;
            this.mastered = true;
        } else if (kind.equalsIgnoreCase("groups")) {
            this.distributed = true;
            this.mastered = false;
        } else {
            throw new RuntimeException("postgres config is invalid, kind is invalid, it must be one of master_slaver or groups");
        }
        this.index = new AtomicInteger();
        List<NodeConfig> nodeConfigs = config.getNodes();
        this.nodes = new ArrayList<>();
        for (NodeConfig nodeConfig : nodeConfigs) {
            this.nodes.add(new DatabaseNode(vertx, nodeConfig, config.getType()));
        }
        vertx.getOrCreateContext().put(context_key, this);
        Duration transactionCacheTTL;
        try {
            transactionCacheTTL = Duration.parse(Optional.ofNullable(config.getTransactionCacheTTL()).orElse("PT10M"));
        } catch (Exception e) {
            log.error("parse transactionCacheTTL failed, transactionCacheTTL = {}, set PT10M default", config.getTransactionCacheTTL(), e);
            transactionCacheTTL = Duration.ofMinutes(10);
        }
        Long transactionCacheMaxSize = Optional.ofNullable(config.getTransactionCacheMaxSize()).orElse(100000L);
        this.transactions = Caffeine.newBuilder()
                .expireAfterWrite(transactionCacheTTL)
                .maximumSize(transactionCacheMaxSize)
                .evictionListener(new TransactionEvictionListener())
                .build();
        this.vertx = vertx;
        this.serviceApplied = false;
        this.transactionCachedTTL = transactionCacheTTL.toMillis();
        this.hostId = Optional.ofNullable(vertx.getOrCreateContext().deploymentID()).orElse("local").trim();
    }

    private final Vertx vertx;
    private final boolean distributed;
    private final boolean mastered;
    private final List<DatabaseNode> nodes;
    private final AtomicInteger index;
    private final Cache<@NonNull String, @NonNull CachedTransaction> transactions;
    private final long transactionCachedTTL;


    private boolean serviceApplied;
    private String hostId;
    private ServiceDiscovery discovery;


    public Future<Void> publish(ServiceDiscovery discovery) {
        this.discovery = discovery;
        Promise<Void> promise = Promise.promise();
        Record record = EventBusService.createRecord(
                DatabaseService.SERVICE_NAME,
                DatabaseService.SERVICE_ADDRESS,
                DatabaseService.class
        );
        this.discovery.publish(record)
                .onSuccess(ar -> {
                    if (log.isDebugEnabled()) {
                        log.debug("publish database succeed, id = {}", ar.getRegistration());
                    }
                    this.hostId = ar.getRegistration();
                    promise.complete();
                })
                .onFailure(e -> {
                    log.error("publish database failed", e);
                    promise.fail("publish database failed");
                });
        return promise.future();
    }

    public Pool getNode() {
        if (!this.distributed) {
            return this.nodes.get(0).getPool();
        }
        if (this.mastered) {
            return this.nodes.get(0).getPool();
        }
        return this.nodes.get(this.index.addAndGet(1) % this.nodes.size()).getPool();
    }

    public Pool getSlaver() {
        if (this.nodes.size() < 2) {
            return null;
        }
        int pos = this.index.addAndGet(1) % this.nodes.size();
        if (pos == 0) {
            pos = 1;
        }
        return this.nodes.get(pos).getPool();
    }

    public Future<Void> check() {
        Promise<Void> promise = Promise.promise();
        List<Future> futures = new ArrayList<>();
        for (DatabaseNode node : this.nodes) {
            Promise<Void> checkPromise = Promise.promise();
            String checkSQL = Optional.ofNullable(node.getCheckSQL()).orElse("").trim();
            if (checkSQL.isBlank()) {
                node.getPool().getConnection()
                        .onSuccess(connection -> {
                            connection.close();
                            checkPromise.complete();
                        })
                        .onFailure(e -> {
                            checkPromise.fail(new Exception(String.format("check database(%s:%d) failed", node.getHost(), node.getPort()), e));
                        });
            } else {
                node.getPool().query(checkSQL).execute()
                        .onSuccess(r -> {
                            checkPromise.complete();
                        })
                        .onFailure(e -> {
                            checkPromise.fail(new Exception(String.format("check database(%s:%d) failed", node.getHost(), node.getPort()), e));
                        });
            }
            futures.add(checkPromise.future());
        }
        CompositeFuture.all(futures)
                .onSuccess(r -> {
                    promise.complete();
                })
                .onFailure(promise::fail);
        return promise.future();
    }

    public Future<Void> close() {
        this.serviceApplied = false;
        if (this.discovery != null) {
            this.discovery.unpublish(this.hostId)
                    .onSuccess(ui -> {
                        if (log.isDebugEnabled()) {
                            log.debug("database unpublish succeed");
                        }
                    })
                    .onFailure(ue -> {
                        log.error("database unpublish failed", ue);
                    });
        }
        Promise<Void> promise = Promise.promise();
        List<Future> futures = new ArrayList<>();
        for (DatabaseNode node : this.nodes) {
            futures.add(node.getPool().close());
        }
        CompositeFuture.all(futures)
                .onSuccess(r -> {
                    this.transactions.cleanUp();
                    promise.complete();
                })
                .onFailure(e -> {
                    this.transactions.cleanUp();
                    promise.fail(e);
                });
        return promise.future();
    }

    public Cache<@NonNull String, @NonNull CachedTransaction> getTransactions() {
        return transactions;
    }

    public boolean isServiceApplied() {
        return serviceApplied;
    }

    public void setServiceApplied(boolean serviceApplied) {
        this.serviceApplied = serviceApplied;
    }

    public long getTransactionCachedTTL() {
        return transactionCachedTTL;
    }

    public String getHostId() {
        return hostId;
    }

    public ServiceDiscovery getDiscovery() {
        return discovery;
    }

}
