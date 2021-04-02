package org.pharosnet.vertx.faas.engine.http.auth.jwt;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;

public class JWTAuthFnImpl implements JWTAuthFn {

    public JWTAuthFnImpl(JWTAuthService service) {
        this.service = service;
    }

    private final JWTAuthService service;

    @Override
    public Future<String> generateToken(JsonObject claims) {
        Promise<String> promise = Promise.promise();
        this.service.generateToken(claims, promise);
        return promise.future();
    }

    @Override
    public Future<String> generateTokenWithOptions(JsonObject claims, JWTOptions options) {
        Promise<String> promise = Promise.promise();
        this.service.generateTokenWithOptions(claims, options, promise);
        return promise.future();
    }

    @Override
    public Future<JWTOptions> getJWTOptions() {
        Promise<JWTOptions> promise = Promise.promise();
        this.service.getJWTOptions(promise);
        return promise.future();
    }

}
