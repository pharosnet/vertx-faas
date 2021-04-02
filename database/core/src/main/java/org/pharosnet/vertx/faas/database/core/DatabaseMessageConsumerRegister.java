package org.pharosnet.vertx.faas.database.core;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.core.components.MessageConsumerRegister;
import org.pharosnet.vertx.faas.database.core.impl.DatabaseServiceImpl;

import java.util.List;
import java.util.stream.Collectors;

public class DatabaseMessageConsumerRegister implements MessageConsumerRegister {

    private List<MessageConsumer<JsonObject>> consumers;

    @Override
    public void register(Vertx vertx) {
        this.consumers = List.of(
                DatabaseServiceImpl.register(vertx)
        );
    }

    @Override
    public Future<Void> unregister() {
        Promise<Void> promise = Promise.promise();
        if (consumers == null) {
            promise.complete();
            return promise.future();
        }

        CompositeFuture compositeFuture = CompositeFuture.all(consumers.stream().map(consumer -> {
            Promise<Void> unregisterPromise = Promise.promise();
            consumer.unregister(unregisterPromise);
            return unregisterPromise.future();
        }).collect(Collectors.toList()));

        compositeFuture.onSuccess(r -> promise.complete());
        compositeFuture.onFailure(promise::fail);

        return promise.future();
    }
}
