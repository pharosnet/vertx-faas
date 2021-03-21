package org.pharosnet.vertx.faas.context;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;
import java.util.UUID;

@DataObject
public class Context {

    public static Context fromRoutingContext(RoutingContext routingContext) {
        Context context = new Context();
        String requestId = Optional.ofNullable(routingContext.request().getHeader("x-request-id")).orElse("").trim();
        if (requestId.length() > 0) {
            context.setId(requestId);
        } else {
            context.setId(UUID.randomUUID().toString());
        }
        if (routingContext.user() != null) {
            User user = routingContext.user();
            context.setPrincipal(user.principal());
            context.setAttributes(user.attributes());
        }
        return context;
    }

    public Context() {
        this.data = new JsonObject();
    }

    public Context(JsonObject jsonObject) {
        this.id = jsonObject.getString("id");
        this.data = jsonObject.getJsonObject("data", new JsonObject());
        if (jsonObject.containsKey("principal")) {
            this.principal = jsonObject.getJsonObject("principal");
        }
        if (jsonObject.containsKey("attributes")) {
            this.attributes = jsonObject.getJsonObject("attributes");
        }
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("id", this.getId());
        jsonObject.put("data", this.getData());
        if (this.principal != null) {
            jsonObject.put("principal", this.getPrincipal());
        }
        if (this.attributes != null) {
            jsonObject.put("attributes", this.getAttributes());
        }
        return jsonObject;
    }

    private String id;

    private JsonObject data;
    private JsonObject principal;
    private JsonObject attributes;

    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }

    public JsonObject getData() {
        if (data == null) {
            data = new JsonObject();
        }
        return data;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }


    public User getUser() {
        if (this.principal != null) {
            return User.create(this.principal, this.attributes);
        }
        return null;
    }

    protected JsonObject getPrincipal() {
        return principal;
    }

    protected void setPrincipal(JsonObject principal) {
        this.principal = principal;
    }

    protected JsonObject getAttributes() {
        return attributes;
    }

    protected void setAttributes(JsonObject attributes) {
        this.attributes = attributes;
    }
}
