package org.pharosnet.vertx.faas.engine.http.auth.jwt;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;

public interface JWTAuthFn {

    static JWTAuthFn proxy(Vertx vertx) {
        return new JWTAuthFnImpl(JWTAuthService.proxy(vertx));
    }

    Future<String> generateToken(JsonObject claims);

    Future<String> generateTokenWithOptions(JsonObject claims, JWTOptions options);

    Future<JWTOptions> getJWTOptions();

}
