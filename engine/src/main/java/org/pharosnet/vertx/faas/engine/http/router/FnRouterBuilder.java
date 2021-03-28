package org.pharosnet.vertx.faas.engine.http.router;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.pharosnet.vertx.faas.codegen.annotation.FnRouter;
import org.pharosnet.vertx.faas.core.commons.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public class FnRouterBuilder {

    private static final Logger log = LoggerFactory.getLogger(FnRouterBuilder.class);

    public FnRouterBuilder(String basePackage) {
        this.basePackage = basePackage;
    }

    private final String basePackage;

    public void build(Vertx vertx, Router router) {
        try {
            List<Class<?>> classes = ClassUtils.scan(basePackage, FnRouter.class);

            if (classes.isEmpty()) {
                throw new Exception("未扫描到 @FnRouter 类");
            }

            for (Class<?> clazz : classes) {
                Constructor constructor = clazz.getConstructor(Vertx.class);
                Object instance = constructor.newInstance(vertx);
                Method method = clazz.getMethod("build", Router.class);
                method.invoke(instance, router);
                if (log.isDebugEnabled()) {
                    log.debug("FaaS 构建HTTP路由成功, {}", clazz);
                }
            }

        } catch (Exception e) {
            log.error("FaaS 构建HTTP路由失败", e);
        }

    }

}
