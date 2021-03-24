package org.pharosnet.vertx.faas.core.context;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class Context {

    public Context() {
        this.data = new JsonObject();
    }

    public Context(JsonObject jsonObject) {
        this.id = jsonObject.getString("id");
        this.data = jsonObject.getJsonObject("data");
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("id", this.id)
                .put("data", this.data);
    }

    private String id;
    private JsonObject data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JsonObject getData() {
        return data;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }

}
