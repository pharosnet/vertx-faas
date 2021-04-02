package org.pharosnet.vertx.faas.core.annotations;


import org.pharosnet.vertx.faas.core.annotations.oas.Info;
import org.pharosnet.vertx.faas.core.annotations.oas.Server;
import org.pharosnet.vertx.faas.core.annotations.oas.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnableOAS {

    String resourcePath() default "webroot/openapi.json";
    String dataPath() default "/openapi.json";
    String webPath() default "/openapi-ui";
    String webStaticResourcePath() default "webroot/swagger-ui";

    Info info();
    Server[] servers() default {};
    Tag[] tags() default {};

}
