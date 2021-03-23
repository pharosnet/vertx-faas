package org.pharosnet.vertx.faas.component.http.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.serviceproxy.ServiceException;
import org.pharosnet.vertx.faas.codegen.annotation.EnableOAS;
import org.pharosnet.vertx.faas.commons.ClassUtils;
import org.pharosnet.vertx.faas.component.http.auth.JwtAuths;
import org.pharosnet.vertx.faas.component.http.config.JwtConfig;
import org.pharosnet.vertx.faas.component.http.config.OpenApiConfig;
import org.pharosnet.vertx.faas.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public abstract class AbstractHttpRouter {

    private static final Logger log = LoggerFactory.getLogger(AbstractHttpRouter.class);

    public abstract void build(Vertx vertx, Router router);

    public void buildOpenApi(Router router, OpenApiConfig config, String basePackage) {
        if (config == null) {
            return;
        }
        String jsonPath = Optional.ofNullable(config.getJsonPath()).orElse("").trim();
        if (jsonPath.length() > 0) {
            String resourcePath;
            try {
                List<Class<?>> enableOASClasses = ClassUtils.scan(basePackage, EnableOAS.class);
                if (enableOASClasses.isEmpty()) {
                    throw new Exception("无法加载到@EnableOAS的类");
                }
                if (enableOASClasses.size() > 1) {
                    throw new Exception("@EnableOAS的类只能有一个");
                }
                EnableOAS enableOAS = enableOASClasses.get(0).getAnnotation(EnableOAS.class);
                resourcePath = enableOAS.resourcePath().trim();
                if (resourcePath.length() == 0) {
                    throw new Exception("@EnableOAS的resourcePath值不能为空。");
                }
            } catch (Exception exception) {
                log.error("配置中已激活OpenAPI，但是创建该路由失败。", exception);
                throw new RuntimeException("配置中已激活OpenAPI，但是创建该路由失败。", exception);
            }
            router.route(jsonPath).handler(StaticHandler.create(resourcePath));
            String webPathPrefix = Optional.ofNullable(config.getWebPathPrefix()).orElse("").trim();
            String webStaticResourcePath = Optional.ofNullable(config.getWebStaticResourcePath()).orElse("").trim();
            if (webPathPrefix.length() > 0 && webStaticResourcePath.length() > 0) {
                if (webPathPrefix.endsWith("/")) {
                    webPathPrefix = webPathPrefix + "*";
                } else {
                    webPathPrefix = webPathPrefix + "/*";
                }
                router.route(webPathPrefix).handler(StaticHandler.create(webStaticResourcePath));
            }
        }
    }

    public void buildFailureHandler(Router router) {
        router.route()
                .failureHandler(ctx -> {
                    if (ctx.failed()) {
                        int errorCode = ctx.statusCode();
                        JsonObject result = new JsonObject();
                        if (ctx.failure() instanceof ServiceException) {
                            ServiceException se = (ServiceException) ctx.failure();
                            errorCode = se.failureCode();
                            result.put("message", se.getMessage());
                            if (!se.getDebugInfo().isEmpty()) {
                                result.put("cause", se.getDebugInfo());
                            }
                        } else {
                            result.put("message", ctx.failure().getMessage());
                            result.put("cause", ctx.failure());
                        }
                        if (errorCode == -1) {
                            errorCode = 500;
                        }
                        ctx.response()
                                .setStatusCode(errorCode)
                                .putHeader("Content-Type", "application/json")
                                .end(result.encode(), "UTF-8");
                    } else {
                        ctx.next();
                    }
                });
    }

    public void buildNotFoundHandler(Router router) {
        router.errorHandler(404, ctx -> {
            JsonObject result = new JsonObject();
            result.put("message", "Not Found");
            ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(result.encode(), "UTF-8");
        });
    }

    public void buildAuth(Vertx vertx, Router router, JwtConfig config) throws Exception {
        JWTAuth jwtAuth = new JwtAuths(vertx, config).getJwtAuth();
        router.route().handler(ctx -> {
            String authorization = Optional.ofNullable(ctx.request().getHeader("Authorization")).orElse("");
            if (authorization.length() == 0) {
                ctx.next();
                return;
            }
            if (!authorization.startsWith("Bearer ")) {
                ctx.fail(new UnauthorizedException("不是JWT！"));
                return;
            }
            String bearer = authorization.substring(7);
            JsonObject authenticateConfig = new JsonObject()
                    .put("jwt", bearer)
                    .put("options", new JsonObject()
                            .put("ignoreExpiration", false)
                            .put("issuer", Optional.ofNullable(config.getIssuer()).orElse("").trim()));

            jwtAuth.authenticate(authenticateConfig)
                    .onSuccess(user -> {
                        if (user.expired()) {
                            log.error("JWT 验证失败 {}, 超时。", bearer);
                            ctx.fail(new UnauthorizedException("超时！"));
                            return;
                        }
                        ctx.setUser(user);
                        ctx.next();
                    })
                    .onFailure(e -> {
                        log.error("JWT 验证失败 {}", bearer, e);
                        ctx.fail(new UnauthorizedException("验证失败！"));
                    });
        });
    }

}
