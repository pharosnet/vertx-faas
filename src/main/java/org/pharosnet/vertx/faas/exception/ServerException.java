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

    public static <T> AsyncResult<T> fail(String reason, Throwable throwable) {
        return Future.failedFuture(new ServerException(throwable));
    }

    public ServerException(Throwable throwable) {
        super(code, message);
        super.getDebugInfo().put("message", throwable.getMessage());
        super.getDebugInfo().put("cause", throwable);
    }

    public ServerException(String reason) {
        super(code, message);
        super.getDebugInfo().put("message", reason);
    }

    public ServerException(String reason, Throwable throwable) {
        super(code, message);
        super.getDebugInfo().put("message", reason);
        super.getDebugInfo().put("cause", throwable);
    }

}

