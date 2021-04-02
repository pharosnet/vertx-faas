package org.pharosnet.vertx.faas.database.api;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.vertx.faas.database.api.QueryResult}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.vertx.faas.database.api.QueryResult} original class using Vert.x codegen.
 */
public class QueryResultConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, QueryResult obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "affected":
          if (member.getValue() instanceof Number) {
            obj.setAffected(((Number)member.getValue()).intValue());
          }
          break;
        case "lastInsertedId":
          if (member.getValue() instanceof Number) {
            obj.setLastInsertedId(((Number)member.getValue()).longValue());
          }
          break;
        case "latency":
          if (member.getValue() instanceof Number) {
            obj.setLatency(((Number)member.getValue()).longValue());
          }
          break;
        case "rows":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<io.vertx.core.json.JsonObject> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                list.add(((JsonObject)item).copy());
            });
            obj.setRows(list);
          }
          break;
      }
    }
  }

  public static void toJson(QueryResult obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(QueryResult obj, java.util.Map<String, Object> json) {
    if (obj.getAffected() != null) {
      json.put("affected", obj.getAffected());
    }
    if (obj.getLastInsertedId() != null) {
      json.put("lastInsertedId", obj.getLastInsertedId());
    }
    json.put("latency", obj.getLatency());
    if (obj.getRows() != null) {
      JsonArray array = new JsonArray();
      obj.getRows().forEach(item -> array.add(item));
      json.put("rows", array);
    }
  }
}
