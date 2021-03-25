package org.pharosnet.vertx.faas.engine.http;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.*;
import io.vertx.ext.web.Router;
import org.pharosnet.vertx.faas.core.commons.Host;
import org.pharosnet.vertx.faas.engine.http.config.HttpConfig;
import org.pharosnet.vertx.faas.engine.http.config.SSLConfig;
import org.pharosnet.vertx.faas.engine.http.router.AbstractHttpRouter;
import org.pharosnet.vertx.faas.engine.http.router.FnRouterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class Http {

    private static final Logger log = LoggerFactory.getLogger(Http.class);

    public Http(Vertx vertx, String basePackage, HttpConfig config) {
        this.vertx = vertx;
        this.basePackage = basePackage;
        this.config = config;
    }

    private final Vertx vertx;
    private final HttpConfig config;
    private HttpServer server;
    private final String basePackage;

    public Future<Void> run(AbstractHttpRouter routerbuilder) {
        Promise<Void> promise = Promise.promise();

        if (routerbuilder == null) {
            promise.fail(new IllegalArgumentException("faas startup failed, routerBuilder is null"));
            return promise.future();
        }

        HttpServerOptions options = new HttpServerOptions();

        if (vertx.isNativeTransportEnabled() && this.config.getNetNative() != null) {
            options.setTcpFastOpen(Optional.ofNullable(this.config.getNetNative().getTcpFastOpen()).orElse(false))
                    .setTcpCork(Optional.ofNullable(this.config.getNetNative().getTcpCork()).orElse(false))
                    .setTcpQuickAck(Optional.ofNullable(this.config.getNetNative().getTcpQuickAck()).orElse(false))
                    .setReusePort(Optional.ofNullable(this.config.getNetNative().getReusePort()).orElse(false));
        }

        options.setPort(this.config.getPort());

        String host;
        try {
            host = Host.get().getIp();
        } catch (Exception e) {
            promise.fail(e);
            return promise.future();
        }
        options.setHost(host);

        options.setLogActivity(Optional.ofNullable(this.config.getEnableLogActivity()).orElse(false));
        options.setAcceptBacklog(Optional.ofNullable(this.config.getBacklog()).orElse(NetServerOptions.DEFAULT_ACCEPT_BACKLOG));
        if (this.config.getCompress() != null) {
            if (log.isDebugEnabled()) {
                log.debug("set compress options. noted!");
            }
            options.setCompressionSupported(Optional.ofNullable(this.config.getCompress().getCompression()).orElse(false));
            options.setDecompressionSupported(Optional.ofNullable(this.config.getCompress().getDecompression()).orElse(false));
        }
        if (this.config.getSsl() != null) {
            if (log.isDebugEnabled()) {
                log.debug("set ssl options. noted!");
            }
            SSLConfig sslConfig = this.config.getSsl();

            boolean hasKey = false;
            String keystore = Optional.ofNullable(sslConfig.getKeystore()).orElse("").trim();
            if (keystore.length() > 0) {
                String password = Optional.ofNullable(sslConfig.getPassword()).orElse("").trim();
                if (password.length() == 0) {
                    log.error("无法创建SSL, password 不能为空。");
                    promise.fail(new Exception("无法创建SSL, password 不能为空。"));
                    return promise.future();
                }
                JksOptions jksOptions = new JksOptions().setPath(keystore).setPassword(password);
                if (Optional.ofNullable(sslConfig.getTrust()).orElse(false)) {
                    options.setTrustStoreOptions(jksOptions);
                } else {
                    options.setKeyStoreOptions(jksOptions);
                }
                hasKey = true;
            }
            String cert = Optional.ofNullable(sslConfig.getCert()).orElse("").trim();
            if (cert.length() > 0) {
                String key = Optional.ofNullable(sslConfig.getKey()).orElse("").trim();
                if (key.length() == 0) {
                    log.error("无法创建SSL, key 不能为空。");
                    promise.fail(new Exception("无法创建SSL, key 不能为空。"));
                    return promise.future();
                }
                options.setKeyCertOptions(new PemKeyCertOptions().setCertPath(cert).setKeyPath(key));

                hasKey = true;
            }

            if (!hasKey) {
                log.error("无法创建SSL, 配置缺失。");
                promise.fail(new Exception("无法创建SSL, 配置缺失。"));
                return promise.future();
            }

            options.removeEnabledSecureTransportProtocol("TLSv1");
            options.addEnabledSecureTransportProtocol("TLSv1.3");
            options.setSsl(true);
            SSLEngineOptions sslEngineOptions;
            if (OpenSSLEngineOptions.isAvailable()) {
                sslEngineOptions = new OpenSSLEngineOptions();
            } else {
                sslEngineOptions = new JdkSSLEngineOptions();
            }
            options.setSslEngineOptions(sslEngineOptions);

            if (Optional.ofNullable(sslConfig.getHttp2()).orElse(false)) {

                if (Optional.ofNullable(sslConfig.getHttp2UseAlpn()).orElse(false)) {
                    options.setUseAlpn(true);
                    options.setAlpnVersions(List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
                }
                int http2WindowSize = Optional.ofNullable(sslConfig.getHttp2WindowSize()).orElse(-1);
                if (http2WindowSize > 0) {
                    options.setHttp2ConnectionWindowSize(http2WindowSize);
                }
            }
        }

        Router router = Router.router(vertx);
        routerbuilder.buildNotFoundHandler(router);
        routerbuilder.buildFailureHandler(router);
        routerbuilder.buildOpenApi(router, basePackage);
        if (config.getJwt() != null) {
            try {
                routerbuilder.buildAuth(vertx, router, config.getJwt());
            } catch (Exception e) {
                log.error("创建JWT路由失败", e);
                promise.fail("创建JWT路由失败");
                return promise.future();
            }
        }
        routerbuilder.build(vertx, router);
        FnRouterBuilder fnRouterBuilder = new FnRouterBuilder(basePackage);
        fnRouterBuilder.build(vertx, router);

        vertx.createHttpServer(options).requestHandler(router).listen(r -> {
            if (r.failed()) {
                log.error("无法启动 HTTP 服务,", r.cause());
                promise.fail(r.cause());
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug("启动 HTTP 服务成功, {}:{}", options.getHost(), options.getPort());
            }
            this.server = r.result();
            // todo discovery
            promise.complete();
        });

        return promise.future();
    }

    public Future<Void> close() {
        Promise<Void> promise = Promise.promise();
        this.server.close(promise);
        return promise.future();
    }

}
