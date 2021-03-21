package org.pharosnet.vertx.faas.component;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.config.FaaSConfig;

import java.util.List;

public abstract class ComponentDeployment {


    public ComponentDeployment() {}

    public ComponentDeployment(MessageConsumerRegister consumers) {
        this.register = consumers;
    }

    private MessageConsumerRegister register;

    public abstract Future<String> deploy(Vertx vertx, FaaSConfig config);

    public MessageConsumerRegister getRegister() {
        return register;
    }

    public void setRegister(MessageConsumerRegister register) {
        this.register = register;
    }
}
