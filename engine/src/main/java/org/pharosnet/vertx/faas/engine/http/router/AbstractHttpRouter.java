package org.pharosnet.vertx.faas.engine.http.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.serviceproxy.ServiceException;
import org.pharosnet.vertx.faas.engine.http.auth.Auth;

public abstract class AbstractHttpRouter {

    public abstract void build(Vertx vertx, Router router);

    public abstract Auth auth();

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

}
