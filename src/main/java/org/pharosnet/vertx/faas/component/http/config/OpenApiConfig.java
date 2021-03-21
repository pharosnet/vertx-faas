package org.pharosnet.vertx.faas.component.http.config;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class OpenApiConfig {

    public OpenApiConfig() {
    }

    public OpenApiConfig(JsonObject jsonObject) {
        OpenApiConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        OpenApiConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private String jsonPath;
    private String webPathPrefix;
    private String webStaticResourcePath;

    public String getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public String getWebPathPrefix() {
        return webPathPrefix;
    }

    public void setWebPathPrefix(String webPathPrefix) {
        this.webPathPrefix = webPathPrefix;
    }

    public String getWebStaticResourcePath() {
        return webStaticResourcePath;
    }

    public void setWebStaticResourcePath(String webStaticResourcePath) {
        this.webStaticResourcePath = webStaticResourcePath;
    }
}
