package org.pharosnet.vertx.faas.core.components;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface MessageConsumerRegister {

    void register(Vertx vertx);

    Future<Void> unregister();

}