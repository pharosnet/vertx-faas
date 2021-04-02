package org.pharosnet.vertx.faas.engine.http.auth.jwt;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import org.pharosnet.vertx.faas.core.commons.FileUtils;
import org.pharosnet.vertx.faas.core.exceptions.UnauthorizedException;
import org.pharosnet.vertx.faas.engine.http.auth.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class JwtAuth implements Auth {

    private static final Logger log = LoggerFactory.getLogger(JwtAuth.class);

    public static JwtAuth create() {
        return new JwtAuth();
    }

    protected JwtAuth() {
    }

    private JWTAuth jwtAuth;

    protected static JWTOptions createJWTOptions(JwtConfig config) {
        JWTOptions jwtOptions = new JWTOptions();

        String algorithm = Optional.ofNullable(config.getAlgorithm()).orElse("").trim().toUpperCase();
        jwtOptions.setAlgorithm(algorithm);

        String exp = Optional.ofNullable(config.getExpirationDuration()).orElse("").trim();
        if (!exp.isBlank()) {
            Duration expirationDuration = Duration.parse(exp);
            jwtOptions.setExpiresInSeconds((int)expirationDuration.toSeconds());
        }
        jwtOptions.setIgnoreExpiration(Optional.ofNullable(config.getExpirationIgnored()).orElse(false));

        jwtOptions.setIssuer(Optional.ofNullable(config.getIssuer()).orElse("Vertx-FaaS").trim());

        List<String> aud = config.getAudience();
        if (aud != null && !aud.isEmpty()) {
            jwtOptions.setAudience(aud);
        }

        jwtOptions.setLeeway(Optional.ofNullable(config.getLeeway()).orElse(0));

        return jwtOptions;
    }

    protected static JWTAuth createJWT(Vertx vertx, JwtConfig config) throws Exception {
        try {
            JWTAuthOptions options = new JWTAuthOptions();
            String algorithm = Optional.ofNullable(config.getAlgorithm()).orElse("").trim().toUpperCase();
            if (algorithm.startsWith("RS")) {
                String pub = FileUtils.getResource(config.getPubKey());
                PubSecKeyOptions pubSecKeyOptions = new PubSecKeyOptions();
                pubSecKeyOptions.setAlgorithm(algorithm)
                        .setBuffer(pub);
                options.addPubSecKey(pubSecKeyOptions);
                String pri = FileUtils.getResource(config.getPriKey());
                PubSecKeyOptions priSecKeyOptions = new PubSecKeyOptions();
                priSecKeyOptions.setAlgorithm(algorithm)
                        .setBuffer(pri);
                options.addPubSecKey(priSecKeyOptions);
            } else if (algorithm.startsWith("HS")) {
                PubSecKeyOptions pubSecKeyOptions = new PubSecKeyOptions();
                pubSecKeyOptions.setAlgorithm(algorithm)
                        .setBuffer(Optional.ofNullable(config.getKey()).orElse("Vertx-FaaS"));
            } else if (algorithm.startsWith("ES")) {
                String pri = FileUtils.getResource(config.getPriKey());
                PubSecKeyOptions priSecKeyOptions = new PubSecKeyOptions();
                priSecKeyOptions.setAlgorithm(algorithm)
                        .setBuffer(pri);
                options.addPubSecKey(priSecKeyOptions);
            } else {
                throw new Exception("create jwt failed, algorithm is not defined");
            }
            return JWTAuth.create(vertx, options);
        } catch (Exception e) {
            log.error("创建JWT失败", e);
            throw new Exception("创建JWT失败", e);
        }
    }

    @Override
    public AuthenticationProvider create(Vertx vertx, JsonObject _config) throws Exception {
        try {
            JwtConfig config = new JwtConfig(_config.getJsonObject("auth"));
            this.jwtAuth = createJWT(vertx, config);
            return this.jwtAuth;
        } catch (Exception e) {
            log.error("创建JWT失败", e);
            throw new Exception("创建JWT失败", e);
        }
    }

    @Override
    public Handler<RoutingContext> handle() {
        return (ctx -> {
            String authorization = Optional.ofNullable(ctx.request().getHeader("Authorization")).orElse("");
            if (authorization.length() == 0) {
                ctx.next();
                return;
            }
            if (!authorization.startsWith("Bearer ")) {
                ctx.fail(new UnauthorizedException("Authorization token is not bearer type!"));
                return;
            }
            String bearer = authorization.substring(7);
            TokenCredentials credentials = new TokenCredentials(bearer);

            this.jwtAuth.authenticate(credentials)
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
