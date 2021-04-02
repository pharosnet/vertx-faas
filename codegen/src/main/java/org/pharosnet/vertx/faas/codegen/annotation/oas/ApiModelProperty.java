package org.pharosnet.vertx.faas.codegen.annotation.oas;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiModelProperty {

    String name() default "";
    String description() default "";
    boolean hidden() default false;

}
