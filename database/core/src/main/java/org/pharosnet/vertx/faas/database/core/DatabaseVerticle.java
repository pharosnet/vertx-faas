package org.pharosnet.vertx.faas.database.core;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.servicediscovery.ServiceDiscovery;
import org.pharosnet.vertx.faas.core.components.MessageConsumerRegister;
import org.pharosnet.vertx.faas.database.core.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(DatabaseVerticle.class);

    public DatabaseVerticle() {
        this.discovery = null;
    }

    public DatabaseVerticle(ServiceDiscovery discovery) {
        this.discovery = discovery;
    }

    private final ServiceDiscovery discovery;
    private MessageConsumerRegister register;

    private Databases databases;


    public void register() {
        this.register = new DatabaseMessageConsumerRegister();
        this.register.register(this.vertx);
    }

    public Future<Void> unregister() {
        if (this.register == null) {
            return Future.succeededFuture();
        }
        return this.register.unregister();
    }


    @Override
    public void start(Promise<Void> promise) throws Exception {

        DatabaseConfig config = new DatabaseConfig(config().getJsonObject("database"));
        this.databases = new Databases(vertx, config);

        this.databases.check()
                .onFailure(e -> {
                    log.error("database verticle start failed, can not connect to database.", e);
                    promise.fail(e);
                })
                .onSuccess(v -> {
                    this.register();
                    if (this.discovery == null) {
                        this.databases.setServiceApplied(true);
                        promise.complete();
                        return;
                    }
                    this.databases.publish(this.discovery)
                            .onSuccess(pr -> {
                                if (log.isDebugEnabled()) {
                                    log.debug("database verticle start succeed");
                                }
                                this.databases.setServiceApplied(true);
                                promise.complete();
                            })
                            .onFailure(pe -> {
                                promise.fail(new Exception("database verticle start failed, publish failed.", pe));
                            });
                });
    }

    @Override
    public void stop(Promise<Void> promise) throws Exception {
        this.unregister()
                .compose(r -> this.databases.close())
                .onSuccess(r -> {
                    if (log.isDebugEnabled()) {
                        log.debug("database verticle stop succeed");
                    }
                    promise.complete();
                })
                .onFailure(e -> {
                    log.error("database verticle stop failed", e);
                    promise.fail("database verticle stop failed");
                });
    }
}
