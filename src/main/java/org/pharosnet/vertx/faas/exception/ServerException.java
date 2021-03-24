package org.pharosnet.vertx.faas.exception;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceException;

public class ServerException extends ServiceException {

    private static final int code = 500;
    private static final String message = "服务错误！";

    public static <T> AsyncResult<T> fail(Throwable throwable) {
        return Future.failedFuture(new ServerException(throwable));
    }

    public static <T> AsyncResult<T> fail(String reason) {
        return Future.failedFuture(new ServerException(reason));
    }

    public static <T> AsyncResult<T> fail(int statusCode, String reason) {
        return Future.failedFuture(new ServerException(statusCode, reason));
    }


    public static <T> AsyncResult<T> fail(String reason, Throwable throwable) {
        return Future.failedFuture(new ServerException(reason, throwable));
    }

    public static <T> AsyncResult<T> fail(int statusCode, String reason, Throwable throwable) {
        return Future.failedFuture(new ServerException(statusCode, reason, throwable));
    }

    public ServerException(Throwable throwable) {
        this(code, throwable);
    }

    public ServerException(int statusCode, Throwable throwable) {
        super(statusCode, message);
        super.getDebugInfo().put("message", throwable.getMessage());
        super.getDebugInfo().put("cause", throwable);
    }

    public ServerException(String reason) {
        this(code, reason);
    }

    public ServerException(int statusCode, String reason) {
        super(statusCode, message);
        super.getDebugInfo().put("message", reason);
    }

    public ServerException(String reason, Throwable throwable) {
        this(code, reason, throwable);
    }

    public ServerException(int statusCode, String reason, Throwable throwable) {
        super(statusCode, message);
        super.getDebugInfo().put("message", reason);
        super.getDebugInfo().put("cause", throwable);
    }

}

