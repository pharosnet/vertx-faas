package org.pharosnet.vertx.faas.component.http.config;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class CompressConfig {

    public CompressConfig() {
    }

    public CompressConfig(JsonObject jsonObject) {
        CompressConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        CompressConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private Boolean compression;
    private Boolean decompression;

    public Boolean getCompression() {
        return compression;
    }

    public void setCompression(Boolean compression) {
        this.compression = compression;
    }

    public Boolean getDecompression() {
        return decompression;
    }

    public void setDecompression(Boolean decompression) {
        this.decompression = decompression;
    }

}
