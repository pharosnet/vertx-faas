package org.pharosnet.vertx.faas.codegen.annotation.oas;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Info {

    String title();
    String description() default "";
    String termsOfService() default "";
    Contact contact();
    License license();
    String version();

}
