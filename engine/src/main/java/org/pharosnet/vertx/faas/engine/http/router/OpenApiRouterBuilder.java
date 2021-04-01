package org.pharosnet.vertx.faas.engine.http.router;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.pharosnet.vertx.faas.codegen.annotation.EnableOAS;
import org.pharosnet.vertx.faas.core.commons.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class OpenApiRouterBuilder {

    private static final Logger log = LoggerFactory.getLogger(OpenApiRouterBuilder.class);

    public OpenApiRouterBuilder(String basePackage) {
        this.basePackage = basePackage;
    }

    private final String basePackage;

    public void build(Router router) {
        String resourcePath;
        String dataPath;
        String webPath;
        String webStaticResourcePath;

        try {
            List<Class<?>> enableOASClasses = ClassUtils.scan(basePackage, EnableOAS.class);
            if (enableOASClasses.isEmpty()) {
                return;
            }
            if (enableOASClasses.size() > 1) {
                throw new Exception("@EnableOAS的类只能有一个");
            }
            EnableOAS enableOAS = enableOASClasses.get(0).getAnnotation(EnableOAS.class);
            resourcePath = enableOAS.resourcePath().trim();
            if (resourcePath.length() == 0) {
                throw new Exception("@EnableOAS的resourcePath值不能为空。");
            }
            dataPath = enableOAS.dataPath().trim();
            if (dataPath.length() == 0) {
                throw new Exception("@EnableOAS的dataPath值不能为空。");
            }
            webPath = enableOAS.webPath().trim();
            webStaticResourcePath = enableOAS.webStaticResourcePath().trim();
        } catch (Exception exception) {
            log.error("配置中已激活OpenAPI，但是创建该路由失败。", exception);
            throw new RuntimeException("配置中已激活OpenAPI，但是创建该路由失败。", exception);
        }

        router.route(dataPath).handler(StaticHandler.create(resourcePath));

        if (webPath.length() > 0 && webStaticResourcePath.length() > 0) {
            String redirectWebPath = webPath;
            if (webPath.endsWith("/")) {
                webPath = webPath + "*";
                redirectWebPath = redirectWebPath.substring(0, redirectWebPath.lastIndexOf("/"));
            } else {
                webPath = webPath + "/*";
            }
            String finalWebPath = redirectWebPath;
            router.get(finalWebPath)
                    .handler(ctx -> {
                        ctx.redirect(String.format("%s/index.html?url=%s", finalWebPath, dataPath));
                    });
            router.route(webPath).handler(StaticHandler.create(webStaticResourcePath));
        }
    }

}
