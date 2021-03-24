package org.pharosnet.vertx.faas.validator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.pharosnet.vertx.faas.exception.ParameterInvalidException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Validators {

    public static void init() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validators.validator = factory.getValidator();
    }

    private static Validator validator;

    public static <T> boolean validateExecutables(Method method, Object instance, Object[] parameterValues, Handler<AsyncResult<T>> handler) {
        Set<ConstraintViolation<Object>> violations = Validators.validator.forExecutables().validateParameters(
                instance,
                method,
                parameterValues
        );

        List<String> messages = null;
        for (ConstraintViolation<Object> violation : violations) {
            if (messages == null) {
                messages = new ArrayList<>();
            }
            String message = violation.getMessage();
            messages.add(message);
        }

        if (messages != null) {
            handler.handle(ParameterInvalidException.fail(messages));
            return false;
        }
        return true;
    }

}
