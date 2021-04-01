package org.pharosnet.vertx.faas.engine.http;

import io.vertx.core.Vertx;

public interface HttpServerHooker {

    void onStart(Vertx vertx, HttpInfo info);

    void onStop(Vertx vertx, HttpInfo info);

}
