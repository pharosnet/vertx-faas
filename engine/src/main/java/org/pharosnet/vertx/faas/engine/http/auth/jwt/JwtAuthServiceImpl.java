package org.pharosnet.vertx.faas.engine.http.auth.jwt;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.serviceproxy.ServiceBinder;
import org.pharosnet.vertx.faas.engine.http.auth.AuthService;

public class JwtAuthServiceImpl implements AuthService {

    public JwtAuthServiceImpl(Vertx vertx, JwtConfig config) throws Exception {
        this.vertx = vertx;
        this.auth = JwtAuth.createJWT(vertx, config);
    }

    private final JWTAuth auth;
    private final Vertx vertx;

    public static MessageConsumer<JsonObject> register(Vertx vertx, JwtConfig config) throws Exception {
        return new ServiceBinder(vertx).setAddress(SERVICE_ADDRESS).register(AuthService.class, new JwtAuthServiceImpl(vertx, config));
    }

    @Override
    public void generateToken(JsonObject claims, Handler<AsyncResult<String>> handler) {
        String token = this.auth.generateToken(claims);
        handler.handle(Future.succeededFuture(token));
    }

}
