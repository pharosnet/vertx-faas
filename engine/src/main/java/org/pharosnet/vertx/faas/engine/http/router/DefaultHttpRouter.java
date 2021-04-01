package org.pharosnet.vertx.faas.engine.http.router;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.pharosnet.vertx.faas.engine.http.auth.Auth;

public class DefaultHttpRouter extends AbstractHttpRouter {

    @Override
    public void build(Vertx vertx, Router router) {

    }

    @Override
    public Auth auth() {
        return null;
    }

}
