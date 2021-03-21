package org.pharosnet.vertx.faas.exception;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceException;

public class UnauthorizedException extends ServiceException {

    private static final int code = 401;
    private static final String message = "验证错误！";

    public static <T> AsyncResult<T> fail(String reason) {
        return Future.failedFuture(new UnauthorizedException(reason));
    }

    public static <T> AsyncResult<T> fail() {
        return Future.failedFuture(new UnauthorizedException());
    }

    public UnauthorizedException(String reason) {
        super(code, message);
        super.getDebugInfo().put("reason", reason);
    }

    public UnauthorizedException() {
        super(code, message);
    }

}