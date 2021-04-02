package org.pharosnet.vertx.faas.database.api;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class TransactionResult {

    public TransactionResult() {
    }

    public TransactionResult(JsonObject jsonObject) {
        TransactionResultConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        TransactionResultConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private long latency;

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }
}
