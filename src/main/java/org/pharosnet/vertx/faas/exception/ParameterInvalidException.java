package org.pharosnet.vertx.faas.exception;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceException;

import java.util.List;

public class ParameterInvalidException extends ServiceException {

    private static final int code = 400;
    private static final String message = "参数错误！";

    public static <T> AsyncResult<T> fail(List<String> messages) {
        return Future.failedFuture(new ParameterInvalidException(messages));
    }

    public static <T> AsyncResult<T> fail(int statusCode, List<String> messages) {
        return Future.failedFuture(new ParameterInvalidException(statusCode, messages));
    }

    public ParameterInvalidException(List<String> messages) {
        this(code, messages);
    }

    public ParameterInvalidException(int statusCode, List<String> messages) {
        super(statusCode, message);
        super.getDebugInfo().put("invalidParameters", messages);
    }

}
