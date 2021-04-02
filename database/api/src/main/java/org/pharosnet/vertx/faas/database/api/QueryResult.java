package org.pharosnet.vertx.faas.database.api;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.util.List;

@DataObject(generateConverter = true)
public class QueryResult {

    public QueryResult() {
    }

    public QueryResult(JsonObject jsonObject) {
        QueryResultConverter.fromJson(jsonObject, this);
    }


    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        QueryResultConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private long latency;
    private Long lastInsertedId;
    private Integer affected;
    private List<JsonObject> rows;

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    public Long getLastInsertedId() {
        return lastInsertedId;
    }

    public void setLastInsertedId(Long lastInsertedId) {
        this.lastInsertedId = lastInsertedId;
    }

    public Integer getAffected() {
        return affected;
    }

    public void setAffected(Integer affected) {
        this.affected = affected;
    }

    public List<JsonObject> getRows() {
        return rows;
    }

    public void setRows(List<JsonObject> rows) {
        this.rows = rows;
    }
}
