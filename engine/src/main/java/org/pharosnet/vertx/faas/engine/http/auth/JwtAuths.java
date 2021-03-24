package org.pharosnet.vertx.faas.engine.http.auth;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import org.pharosnet.vertx.faas.core.commons.FileUtils;
import org.pharosnet.vertx.faas.engine.http.config.JwtConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

public class JwtAuths {

    private static final Logger log = LoggerFactory.getLogger(JwtAuths.class);

    private static JwtAuths instance = null;

    public static JWTAuth get() {
        return instance.getJwtAuth();
    }

    public JwtAuths(Vertx vertx, JwtConfig config) throws Exception {
        this.config = config;
        JWTAuthOptions options = new JWTAuthOptions();
        if ("RS256".equalsIgnoreCase(config.getAlgorithm())) {
            try {
                String pub = FileUtils.getResource(config.getPubKey());
                String pri = FileUtils.getResource(config.getPriKey());
                options.addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("RS256")
                        .setBuffer(pub))
                        .addPubSecKey(new PubSecKeyOptions()
                                .setAlgorithm("RS256")
                                .setBuffer(pri));

            } catch (Exception e) {
                log.error("创建JWT失败", e);
                throw new Exception("创建JWT失败", e);
            }
        }
        this.jwtAuth = JWTAuth.create(vertx, options);
        instance = this;
    }

    private JWTAuth jwtAuth;
    private JwtConfig config;

    public JWTAuth getJwtAuth() {
        return jwtAuth;
    }

    public void setJwtAuth(JWTAuth jwtAuth) {
        this.jwtAuth = jwtAuth;
    }

    public JwtConfig getConfig() {
        return config;
    }

    public void setConfig(JwtConfig config) {
        this.config = config;
    }

    public String generateToken(User user) {
        int exp = (int) Duration.parse(this.config.getExpirationDuration()).getSeconds();
        return this.jwtAuth.generateToken(user.principal(),
                new JWTOptions()
                        .setIssuer(Optional.ofNullable(this.config.getIssuer()).orElse("").trim())
                        .setSubject(user.get("username"))
                        .setExpiresInSeconds(exp)
        );

    }

}
