package org.pharosnet.vertx.faas.engine.http.config;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class HttpDiscoveryConfig {

    public HttpDiscoveryConfig() {
    }

    public HttpDiscoveryConfig(JsonObject jsonObject) {
        HttpDiscoveryConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        HttpDiscoveryConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    // consul, kubernetes, docker
    private String type;

    private JsonObject config;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonObject getConfig() {
        return config;
    }

    public void setConfig(JsonObject config) {
        this.config = config;
    }

}
