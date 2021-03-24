package org.pharosnet.vertx.faas.codegen.annotation;


import org.pharosnet.vertx.faas.component.http.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Fn {

    int succeedStatus() default 200;

    HttpMethod method() default HttpMethod.POST;

    String path() default "";

    String summary() default "";

    String description() default "";

    boolean authentication() default false;

    String consumes() default "";

    String produces() default "application/json";

    long bodyLimit() default 4194304;

    long timeout() default 5000L;

    boolean latency() default true;

    String[] tags() default {};

    String implementClassFullName();
    String[] beforeHandleInterceptors() default {};
    String[] afterHandleInterceptors() default {};

}
