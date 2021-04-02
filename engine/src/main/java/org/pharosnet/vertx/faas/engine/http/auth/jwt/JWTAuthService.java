package org.pharosnet.vertx.faas.engine.http.auth.jwt;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;

@VertxGen
@ProxyGen
public interface JWTAuthService {

    String SERVICE_ADDRESS = "vertx-faas-engine-http-JWTAuthService";

    static JWTAuthService proxy(Vertx vertx) {
        return new JWTAuthServiceVertxEBProxy(vertx, SERVICE_ADDRESS);
    }

    void generateToken(JsonObject claims, Handler<AsyncResult<String>> handler);

    void generateTokenWithOptions(JsonObject claims, JWTOptions options, Handler<AsyncResult<String>> handler);

    void getJWTOptions(Handler<AsyncResult<JWTOptions>> handler);

}
