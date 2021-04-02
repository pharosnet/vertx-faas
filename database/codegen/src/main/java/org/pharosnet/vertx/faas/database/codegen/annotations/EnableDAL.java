package org.pharosnet.vertx.faas.database.codegen.annotations;

import org.pharosnet.vertx.faas.database.codegen.DatabaseType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnableDAL {

    DatabaseType type() default DatabaseType.POSTGRES;

}
