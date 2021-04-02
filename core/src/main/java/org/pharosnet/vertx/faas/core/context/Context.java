package org.pharosnet.vertx.faas.core.context;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.UUID;

@DataObject
public class Context {

    public Context(String id) {
        if (id == null) {
            throw new IllegalArgumentException("new context failed, id is null.");
        }
        this.id = id;
        this.data = new JsonObject();
    }

    public Context(JsonObject jsonObject) {
        String id = Optional.ofNullable(jsonObject.getString("id")).orElse("").trim();
        if (id.isBlank()) {
            throw new IllegalArgumentException("new context failed, id is null.");
        }
        this.id = id;
        this.data = jsonObject.getJsonObject("data");
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("id", this.id)
                .put("data", this.data);
    }

    private String id;
    private JsonObject data;
    private Context parent;

    public Context fork() {
        return this.fork(UUID.randomUUID().toString());
    }

    public Context fork(String id) {
        Context child = new Context(id);
        child.setParent(this);
        child.data.mergeIn(this.data, true);
        return child;
    }

    public void join(Context parent) {
        if (parent == null) {
            throw new IllegalArgumentException("context join failed, parent context is null.");
        }
        this.setParent(parent);
        this.data.mergeIn(parent.data, true);
    }

    public String rootId() {
        if (this.parent != null) {
            return this.parent.rootId();
        }
        return this.id;
    }

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

    public Context getParent() {
        return parent;
    }

    protected void setParent(Context parent) {
        this.parent = parent;
    }

    public void mergeData(JsonObject otherData) {
        this.data.mergeIn(otherData, true);
    }
}
