package org.pharosnet.vertx.faas.commons;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class MessageConsumers {

    public static Future<Void> unregister(List<MessageConsumer<JsonObject>> consumers) {
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
