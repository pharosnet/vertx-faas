package org.pharosnet.vertx.faas.database.api;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.vertx.faas.database.api.QueryArg}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.vertx.faas.database.api.QueryArg} original class using Vert.x codegen.
 */
public class QueryArgConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, QueryArg obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "args":
          if (member.getValue() instanceof JsonArray) {
            obj.setArgs(((JsonArray)member.getValue()).copy());
          }
          break;
        case "batch":
          if (member.getValue() instanceof Boolean) {
            obj.setBatch((Boolean)member.getValue());
          }
          break;
        case "needLastInsertedId":
          if (member.getValue() instanceof Boolean) {
            obj.setNeedLastInsertedId((Boolean)member.getValue());
          }
          break;
        case "query":
          if (member.getValue() instanceof String) {
            obj.setQuery((String)member.getValue());
          }
          break;
        case "slaverMode":
          if (member.getValue() instanceof Boolean) {
            obj.setSlaverMode((Boolean)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(QueryArg obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(QueryArg obj, java.util.Map<String, Object> json) {
    if (obj.getArgs() != null) {
      json.put("args", obj.getArgs());
    }
    if (obj.getBatch() != null) {
      json.put("batch", obj.getBatch());
    }
    if (obj.getNeedLastInsertedId() != null) {
      json.put("needLastInsertedId", obj.getNeedLastInsertedId());
    }
    if (obj.getQuery() != null) {
      json.put("query", obj.getQuery());
    }
    if (obj.getSlaverMode() != null) {
      json.put("slaverMode", obj.getSlaverMode());
    }
  }
}
