package org.pharosnet.vertx.faas.engine.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.pharosnet.vertx.faas.core.discovery.ServiceDiscoveryProvider;
import org.pharosnet.vertx.faas.engine.http.config.HttpConfig;
import org.pharosnet.vertx.faas.engine.http.router.AbstractHttpRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class HttpVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(HttpVerticle.class);

    public HttpVerticle(AbstractHttpRouter httpRouter, HttpServerHooker hooker) {
        this.httpRouter = httpRouter;
        this.hooker = hooker;
    }

    private String id;
    private HttpInfo info;
    private Http http;
    private Optional<ServiceDiscovery> discovery;
    private final AbstractHttpRouter httpRouter;
    private final HttpServerHooker hooker;

    @Override
    public void start(Promise<Void> promise) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("NATIVE 开启 {}", vertx.isNativeTransportEnabled());
        }
        this.discovery = ServiceDiscoveryProvider.get();

        String basePackage = this.config().getJsonObject("_faasOptions").getString("basePackage");
        this.http = new Http(this.vertx, basePackage, this.config());

        this.http.run(this.httpRouter)
                .onSuccess(httpInfo -> {
                    this.info = httpInfo;
                    if (this.discovery.isEmpty()) {
                        if (this.hooker != null) {
                            this.hooker.onStart(vertx, httpInfo);
                        }
                        promise.complete();
                        return;
                    }
                    Record record = HttpEndpoint.createRecord(httpInfo.getName(), httpInfo.getHost(), httpInfo.getPort(), httpInfo.getRootPath());
                    this.discovery.get()
                            .publish(record)
                            .onSuccess(r -> {
                                if (log.isDebugEnabled()) {
                                    log.debug("publish http service succeed, id = {}", r.getRegistration());
                                }
                                this.id = r.getRegistration();
                                if (this.hooker != null) {
                                    this.hooker.onStart(vertx, httpInfo);
                                }
                                promise.complete();
                            })
                            .onFailure(e -> {
                                log.error("publish http service failed.", e);
                                this.http.close();
                                if (this.hooker != null) {
                                    this.hooker.onStop(vertx, httpInfo);
                                }
                                promise.fail("publish http service failed");
                            });
                })
                .onFailure(e -> {
                    log.error("启动HTTP服务失败。", e);
                    promise.fail("启动HTTP服务失败。");
                });
    }

    @Override
    public void stop(Promise<Void> promise) throws Exception {
        if (this.discovery.isEmpty()) {
            this.http.close()
                    .onSuccess(r -> {
                        if (this.hooker != null) {
                            this.hooker.onStop(vertx, this.info);
                        }
                        promise.complete();
                    })
                    .onFailure(e -> {
                        log.error("关闭HTTP服务失败。", e);
                        promise.fail("关闭HTTP服务失败。");
                    });
            return;
        }
        this.discovery.get()
                .unpublish(this.id)
                .compose(v -> this.http.close())
                .onSuccess(r -> {
                    if (this.hooker != null) {
                        this.hooker.onStop(vertx, this.info);
                    }
                    promise.complete();
                })
                .onFailure(e -> {
                    log.error("关闭HTTP服务失败。", e);
                    promise.fail("关闭HTTP服务失败。");
                });

    }

}
