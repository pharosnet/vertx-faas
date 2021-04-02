package org.pharosnet.vertx.faas.engine.http.auth.jwt;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.serviceproxy.ServiceException;

public class JwtAuthServiceImpl implements JWTAuthService {

    public JwtAuthServiceImpl(Vertx vertx, JwtConfig config) throws Exception {
        this.vertx = vertx;
        this.auth = JwtAuth.createJWT(vertx, config);
        this.jwtOptions = JwtAuth.createJWTOptions(config);
    }

    private final Vertx vertx;
    private final JWTAuth auth;
    private final JWTOptions jwtOptions;

    public static MessageConsumer<JsonObject> register(Vertx vertx, JwtConfig config) throws Exception {
        return new ServiceBinder(vertx).setAddress(SERVICE_ADDRESS).register(JWTAuthService.class, new JwtAuthServiceImpl(vertx, config));
    }

    @Override
    public void generateToken(JsonObject claims, Handler<AsyncResult<String>> handler) {
        this.getJWTOptions(r -> {
            if (r.failed()) {
                handler.handle(ServiceException.fail(500, "get jwt options failed", new JsonObject().put("cause", r.cause())));
                return;
            }
            this.generateTokenWithOptions(claims, r.result(), handler);
        });
    }

    @Override
    public void generateTokenWithOptions(JsonObject claims, JWTOptions options, Handler<AsyncResult<String>> handler) {
        String token = this.auth.generateToken(claims, options);
        handler.handle(Future.succeededFuture(token));
    }

    @Override
    public void getJWTOptions(Handler<AsyncResult<JWTOptions>> handler) {
        JsonObject jsonObject = this.jwtOptions.toJson();
        handler.handle(Future.succeededFuture(new JWTOptions(jsonObject.copy())));
    }

}
