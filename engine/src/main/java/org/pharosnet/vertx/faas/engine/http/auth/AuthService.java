package org.pharosnet.vertx.faas.engine.http.auth;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface AuthService {

    String SERVICE_ADDRESS = "vertx-faas-engine-http-AuthService";

    static AuthService proxy(Vertx vertx) {
        return new AuthServiceVertxEBProxy(vertx, SERVICE_ADDRESS);
    }

    void generateToken(JsonObject claims, Handler<AsyncResult<String>> handler);

}
