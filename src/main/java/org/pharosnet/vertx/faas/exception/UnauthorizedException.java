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

    public static <T> AsyncResult<T> fail(int statusCode, String reason) {
        return Future.failedFuture(new UnauthorizedException(statusCode, reason));
    }

    public static <T> AsyncResult<T> fail() {
        return Future.failedFuture(new UnauthorizedException());
    }

    public static <T> AsyncResult<T> fail(int statusCode) {
        return Future.failedFuture(new UnauthorizedException(statusCode));
    }

    public UnauthorizedException(String reason) {
        this(code, reason);
    }

    public UnauthorizedException(int statusCode, String reason) {
        super(statusCode, message);
        super.getDebugInfo().put("reason", reason);
    }

    public UnauthorizedException(int statusCode) {
        super(statusCode, message);
    }

    public UnauthorizedException() {
        this(code);
    }

}