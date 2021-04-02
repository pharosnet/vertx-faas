package org.pharosnet.vertx.faas.core.components;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public abstract class ComponentDeployment {

    public ComponentDeployment() {
    }

    public abstract Future<String> deploy(Vertx vertx, JsonObject config);

}
