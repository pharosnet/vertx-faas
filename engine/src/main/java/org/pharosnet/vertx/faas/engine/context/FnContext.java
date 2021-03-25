package org.pharosnet.vertx.faas.engine.context;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.pharosnet.vertx.faas.core.context.Context;

import java.util.Optional;
import java.util.UUID;

@DataObject
public class FnContext extends Context {

    public static FnContext fromRoutingContext(RoutingContext routingContext) {
        FnContext context;
        String requestId = Optional.ofNullable(routingContext.request().getHeader("x-request-id")).orElse("").trim();
        if (requestId.length() > 0) {
            context = new FnContext(requestId);
        } else {
            context = new FnContext(UUID.randomUUID().toString());
        }
        if (routingContext.user() != null) {
            User user = routingContext.user();
            context.setPrincipal(user.principal());
            context.setAttributes(user.attributes());
        }
        return context;
    }

    public FnContext(String id) {
        super(id);
    }

    public FnContext(JsonObject jsonObject) {
        super(jsonObject);
        if (jsonObject.containsKey("principal")) {
            this.principal = jsonObject.getJsonObject("principal");
        }
        if (jsonObject.containsKey("attributes")) {
            this.attributes = jsonObject.getJsonObject("attributes");
        }
    }

    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = super.toJson();
        if (this.principal != null) {
            jsonObject.put("principal", this.principal);
        }
        if (this.attributes != null) {
            jsonObject.put("attributes", this.attributes);
        }
        return jsonObject;
    }

    private JsonObject principal;
    private JsonObject attributes;

    public JsonObject getPrincipal() {
        return principal;
    }

    public void setPrincipal(JsonObject principal) {
        this.principal = principal;
    }

    public JsonObject getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonObject attributes) {
        this.attributes = attributes;
    }

    public User getUser() {
        if (this.principal != null) {
            return User.create(this.principal, this.attributes);
        }
        return null;
    }

}
