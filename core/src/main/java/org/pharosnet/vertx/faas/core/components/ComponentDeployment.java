package org.pharosnet.vertx.faas.core.components;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public abstract class ComponentDeployment {

    public ComponentDeployment() {
    }

    public ComponentDeployment(MessageConsumerRegister register) {
        this.register = register;
    }

    private MessageConsumerRegister register;

    public abstract Future<String> deploy(Vertx vertx, JsonObject config);

    public MessageConsumerRegister getRegister() {
        return register;
    }

    public void setRegister(MessageConsumerRegister register) {
        this.register = register;
    }

}
