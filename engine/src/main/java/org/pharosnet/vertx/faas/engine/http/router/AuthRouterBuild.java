package org.pharosnet.vertx.faas.engine.http.router;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import org.pharosnet.vertx.faas.engine.http.auth.Auth;
import org.pharosnet.vertx.faas.engine.http.auth.jwt.JwtAuthServiceImpl;
import org.pharosnet.vertx.faas.engine.http.auth.jwt.JwtConfig;

public class AuthRouterBuild {

    public MessageConsumer<JsonObject> build(Vertx vertx, JsonObject config, Router router, Auth auth) throws Exception {
        if (auth == null) {
            return null;
        }
        AuthenticationProvider authenticationProvider = auth.create(vertx, config);
        router.route().handler(auth.handle());

        if (authenticationProvider instanceof JWTAuth) {
            return JwtAuthServiceImpl.register(vertx, new JwtConfig(config.getJsonObject("auth")));
        }

        return null;
    }
}
