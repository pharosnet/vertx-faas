package org.pharosnet.vertx.faas.engine.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.core.components.MessageConsumerRegister;
import org.pharosnet.vertx.faas.engine.http.config.HttpConfig;
import org.pharosnet.vertx.faas.engine.http.router.AbstractHttpRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class HttpVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(HttpVerticle.class);

    public HttpVerticle(MessageConsumerRegister register, AbstractHttpRouter httpRouter) {
        this.register = register;
        this.httpRouter = httpRouter;
    }

    private Http http;
    private final AbstractHttpRouter httpRouter;
    private final MessageConsumerRegister register;
    private List<MessageConsumer<JsonObject>> consumers;

    public void register() {
        if (this.register == null) {
            return;
        }
        this.consumers = register.register(this.vertx);
    }

    public Future<Void> unregister() {
        Promise<Void> promise = Promise.promise();
        if (consumers == null) {
            promise.complete();
            return promise.future();
        }

        CompositeFuture compositeFuture = CompositeFuture.all(consumers.stream().map(consumer -> {
            Promise<Void> unregisterPromise = Promise.promise();
            consumer.unregister(unregisterPromise);
            return unregisterPromise.future();
        }).collect(Collectors.toList()));

        compositeFuture.onSuccess(r -> promise.complete());
        compositeFuture.onFailure(promise::fail);

        return promise.future();
    }

    @Override
    public void start(Promise<Void> promise) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("NATIVE 开启 {}", vertx.isNativeTransportEnabled());
        }
        String basePackage = this.config().getJsonObject("_faasOptions").getString("basePackage");
        this.http = new Http(this.vertx, basePackage, HttpConfig.mapFrom(this.config().getJsonObject("http")));

        this.http.run(this.httpRouter)
                .onSuccess(r -> {
                    this.register();
                    promise.complete();
                })
                .onFailure(e -> {
                    log.error("启动HTTP服务失败。", e);
                    promise.fail("启动HTTP服务失败。");
                });
    }

    @Override
    public void stop(Promise<Void> promise) throws Exception {
        this.unregister()
                .compose(r -> this.http.close())
                .onSuccess(r -> {
                    promise.complete();
                })
                .onFailure(e -> {
                    log.error("关闭HTTP服务失败。", e);
                    promise.fail("关闭HTTP服务失败。");
                });

    }

}
