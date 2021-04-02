package org.pharosnet.vertx.faas.engine.http.auth;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.RoutingContext;

public interface Auth {

    AuthenticationProvider create(Vertx vertx, JsonObject config) throws Exception;

    Handler<RoutingContext> handle();

}
