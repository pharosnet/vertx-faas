package org.pharosnet.vertx.faas.core.exceptions;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceException;

public class SystemException extends ServiceException {

    private static final int code = 503;
    private static final String message = "系统错误！";

    public static <T> AsyncResult<T> fail(Throwable throwable) {
        return Future.failedFuture(new SystemException(throwable));
    }

    public SystemException(Throwable throwable) {
        super(code, message);
        super.getDebugInfo().put("message", throwable.getMessage());
        super.getDebugInfo().put("cause", throwable);
    }

}
