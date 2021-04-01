package org.pharosnet.vertx.faas.engine;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.pharosnet.vertx.faas.codegen.annotation.FnDeployment;
import org.pharosnet.vertx.faas.core.commons.ClassUtils;
import org.pharosnet.vertx.faas.core.commons.Host;
import org.pharosnet.vertx.faas.core.components.ComponentDeployment;
import org.pharosnet.vertx.faas.core.discovery.ServiceDiscoveryInstance;
import org.pharosnet.vertx.faas.core.discovery.ServiceDiscoveryProvider;
import org.pharosnet.vertx.faas.engine.config.Config;
import org.pharosnet.vertx.faas.engine.http.HttpDeployment;
import org.pharosnet.vertx.faas.engine.http.HttpServerHooker;
import org.pharosnet.vertx.faas.engine.http.router.AbstractHttpRouter;
import org.pharosnet.vertx.faas.engine.http.router.DefaultHttpRouter;
import org.pharosnet.vertx.faas.engine.validator.Validators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FaaSEngine {

    private static final Logger log = LoggerFactory.getLogger(FaaSEngine.class);

    public FaaSEngine() {
        this.init();
    }

    private Vertx vertx;
    private String basePackage;

    private List<ComponentDeployment> deployments;
    private ServiceDiscoveryProvider discoveryProvider;
    private HttpServerHooker hooker;

    private Vertx vertx() {
        return vertx;
    }

    protected void init() {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        System.setProperty("hazelcast.logging.type", "slf4j");
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

        Validators.init();
        DatabindCodec.mapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());

        StackTraceElement[] traceElements = Thread.currentThread().getStackTrace();
        if (traceElements.length < 4) {
            throw new RuntimeException("无法获取调用栈信息");
        }
        String invokerClassName = Optional.of(traceElements[3].getClassName()).orElse("").trim();
        if (invokerClassName.length() == 0) {
            throw new RuntimeException("无法获取调用者类信息");
        }
        try {
            Class invokerClass = Class.forName(invokerClassName);
            this.basePackage = invokerClass.getPackageName();
            if (log.isDebugEnabled()) {
                log.debug("获取调用者包 {}", basePackage);
            }
        } catch (Exception e) {
            log.error("无法获取调用者类，类名为{}", invokerClassName);
            throw new RuntimeException("无法获取调用者类，类名为" + invokerClassName, e);
        }
        try {
            this.deployments = this.scanFnDeployment();
            if (log.isDebugEnabled()) {
                log.debug("获取调用者包 {}", basePackage);
            }
        } catch (Exception e) {
            log.error("FaaS注册FnDeployment失败", e);
            throw new RuntimeException("FaaS注册FnDeployment失败", e);
        }
    }

    private List<ComponentDeployment> scanFnDeployment() throws Exception {
        List<ComponentDeployment> componentDeployments = new ArrayList<>();
        try {
            List<Class<?>> classes = ClassUtils.scan(basePackage, FnDeployment.class);
            if (classes.isEmpty()) {
                throw new Exception("未扫描到 @FnDeployment 类");
            }
            for (Class<?> clazz : classes) {
                Constructor constructor = clazz.getConstructor();
                Object instance = constructor.newInstance();
                if (instance instanceof ComponentDeployment) {
                    componentDeployments.add((ComponentDeployment) instance);
                } else {
                    throw new Exception("扫描到 @FnDeployment 类 " + clazz + " 不是继承ComponentDeployment");
                }
            }
        } catch (Exception e) {
            log.error("FaaS注册FnDeployment失败", e);
            throw new Exception("FaaS注册FnDeployment失败", e);
        }
        return componentDeployments;
    }

    public FaaSEngine addDeployment(ComponentDeployment deployment) {
        if (deployment == null) {
            throw new IllegalArgumentException("deployment is null");
        }
        if (this.deployments == null) {
            this.deployments = new ArrayList<>();
        }
        this.deployments.add(deployment);
        return this;
    }

    private Future<Void> createVertx() {
        Promise<Void> promise = Promise.promise();

        VertxOptions vertxOptions = new VertxOptions();

        boolean nativeTransported = Optional.ofNullable(System.getenv("FAAS_NATIVE")).orElse("false").strip().equalsIgnoreCase("true");
        if (nativeTransported) {
            if (log.isDebugEnabled()) {
                log.debug("set prefer native transport true");
            }
            vertxOptions.setPreferNativeTransport(true);
        }

        Integer eventLoops = Optional.ofNullable(Integer.getInteger(Optional.ofNullable(System.getenv("FAAS_EVENT_LOOP")).orElse("0").strip())).orElse(0);

        if (eventLoops > 0) {
            if (log.isDebugEnabled()) {
                log.debug("set event loop pool size be {}", eventLoops);
            }
            vertxOptions.setEventLoopPoolSize(eventLoops);
        }

        boolean clusterEnabled = Optional.ofNullable(System.getenv("FAAS_CLUSTER")).orElse("false").trim().equalsIgnoreCase("true");
        if (clusterEnabled) {
            String clusterConfigPath = Optional.ofNullable(System.getenv("FAAS_CLUSTER_CONFIG")).orElse("").trim();
            if (clusterConfigPath.length() == 0) {
                promise.fail("System env named FAAS_CLUSTER_CONFIG is empty.");
                return promise.future();
            }
            System.setProperty("vertx.hazelcast.config", clusterConfigPath);
            ClusterManager clusterManager = new HazelcastClusterManager();
            vertxOptions.setClusterManager(clusterManager);
            // event bus
            EventBusOptions eventBusOptions = new EventBusOptions();
            String clusterEventBusHost = Optional.ofNullable(System.getenv("FAAS_CLUSTER_EB_HOST")).orElse("").trim();
            if (clusterEventBusHost.isBlank()) {
                try {
                    clusterEventBusHost = Host.get().getIp();
                } catch (Exception e) {
                    throw new RuntimeException("can not get cluster event bus public host, please set FAAS_CLUSTER_EB_HOST env value", e);
                }
            }
            eventBusOptions.setClusterPublicHost(clusterEventBusHost);
            String clusterEventBusPortValue = Optional.ofNullable(System.getenv("FAAS_CLUSTER_EB_PORT")).orElse("").trim();
            if (!clusterEventBusPortValue.isBlank()) {
                try {
                    int clusterEventBusPort = Integer.parseInt(clusterEventBusPortValue);
                    eventBusOptions.setClusterPublicPort(clusterEventBusPort);

                } catch (Exception e) {
                    throw new RuntimeException("can not get cluster event bus public host, please set FAAS_CLUSTER_EB_HOST env value", e);
                }
            }

            Vertx.clusteredVertx(vertxOptions, r -> {
                if (r.succeeded()) {
                    this.vertx = r.result();
                    promise.complete();
                } else {
                    promise.fail(new Exception("创建Cluster失败！", r.cause()));
                }
            });
        } else {
            this.vertx = Vertx.vertx(vertxOptions);
            promise.complete();
        }

        return promise.future();
    }

    public Future<Future<Void>> start() {
        return this.start(new DefaultHttpRouter());
    }

    public Future<Future<Void>> start(AbstractHttpRouter router) {
        Promise<Future<Void>> promise = Promise.promise();

        this.createVertx()
                .onSuccess(r -> {
                    if (log.isDebugEnabled()) {
                        log.debug("native transport: {}", vertx.isNativeTransportEnabled());
                    }
                    Config.read(vertx)
                            .compose(config -> {
                                if (this.discoveryProvider == null) {
                                    return Future.succeededFuture(config);
                                }
                                try {
                                    ServiceDiscovery discovery = this.discoveryProvider.create(vertx, config.getJsonObject("discovery"));
                                    if (discovery == null) {
                                        throw new Exception("create service discovery failed, it has not be defined.");
                                    }
                                    ServiceDiscoveryInstance.set(discovery);
                                } catch (Exception e) {
                                    log.error("create service discovery failed.", e);
                                    return Future.failedFuture(e);
                                }
                                return Future.succeededFuture(config);
                            })
                            .compose(config -> {
                                config.put("_faasOptions", new JsonObject().put("basePackage", this.basePackage));
                                List<Future> deploymentFutures = new ArrayList<>();
                                if (this.deployments != null && !this.deployments.isEmpty()) {
                                    deployments.forEach(deployment -> deploymentFutures.add(deployment.deploy(this.vertx, config)));
                                }
                                deploymentFutures.add(new HttpDeployment(router, this.hooker).deploy(this.vertx, config));
                                return CompositeFuture.join(deploymentFutures);
                            })
                            .onSuccess(compositeFuture -> {
                                List<String> deploymentIds = compositeFuture.list();
                                if (log.isDebugEnabled()) {
                                    log.debug("已发布服务，{}", deploymentIds);
                                }
                                FaaSEngineCloseHooker hooker = new FaaSEngineCloseHooker(vertx, deploymentIds);
                                Runtime.getRuntime().addShutdownHook(hooker);
                                promise.complete(hooker.closeCallback());
                            })
                            .onFailure(e -> {
                                log.error("部署失败", e);
                                promise.fail(e);
                            });
                })
                .onFailure(e -> {
                    log.error("启动失败！", e);
                    promise.fail(new Exception("启动失败！", e));
                });

        return promise.future();
    }

    public void setDiscoveryProvider(ServiceDiscoveryProvider discoveryProvider) {
        this.discoveryProvider = discoveryProvider;
    }

    public void setHooker(HttpServerHooker hooker) {
        this.hooker = hooker;
    }

}
