package org.pharosnet.vertx.faas.database.api;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class QueryArg {

    public QueryArg() {
    }

    public QueryArg(JsonObject jsonObject) {
        QueryArgConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        QueryArgConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private String query;
    private JsonArray args;
    private Boolean batch;
    private Boolean slaverMode;
    private Boolean needLastInsertedId;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public JsonArray getArgs() {
        return args;
    }

    public void setArgs(JsonArray args) {
        this.args = args;
    }

    public Boolean getBatch() {
        return batch;
    }

    public void setBatch(Boolean batch) {
        this.batch = batch;
    }

    public Boolean getSlaverMode() {
        return slaverMode;
    }

    public void setSlaverMode(Boolean slaverMode) {
        this.slaverMode = slaverMode;
    }

    public Boolean getNeedLastInsertedId() {
        return needLastInsertedId;
    }

    public void setNeedLastInsertedId(Boolean needLastInsertedId) {
        this.needLastInsertedId = needLastInsertedId;
    }

    public String toString() {
        return this.toJson().encodePrettily();
    }

}
