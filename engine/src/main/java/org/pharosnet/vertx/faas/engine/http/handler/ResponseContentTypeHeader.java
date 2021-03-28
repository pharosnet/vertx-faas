package org.pharosnet.vertx.faas.engine.http.handler;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class ResponseContentTypeHeader implements Handler<RoutingContext> {

    public static ResponseContentTypeHeader create(String contentType) {
        return new ResponseContentTypeHeader(contentType);
    }

    public ResponseContentTypeHeader(String contentType) {
        this.contentType = contentType;
    }

    private final String contentType;

    @Override
    public void handle(RoutingContext rc) {
        rc.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
        rc.next();
    }
}
