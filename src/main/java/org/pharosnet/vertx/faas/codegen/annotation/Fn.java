package org.pharosnet.vertx.faas.codegen.annotation;


import org.pharosnet.vertx.faas.component.http.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Fn {

    // operator id = ClassName
    HttpMethod method();

    String path();

    String description() default "";

    boolean authentication() default false;

    String consumes() default "";

    String produces() default "application/json";

    long bodyLimit() default 4194304;

    long timeout() default 5000L;

    boolean latency() default false;

    String[] tags() default {};

    String implementClassFullName();

}
