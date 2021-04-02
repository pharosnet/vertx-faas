package org.pharosnet.vertx.faas.codegen.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface FnModule {

    String name();
    String description() default "";

    int workers() default 0;
    int instances() default 1;

}
