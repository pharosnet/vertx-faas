package org.pharosnet.vertx.faas.database.core.config;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.vertx.faas.database.core.config.DatabaseConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.vertx.faas.database.core.config.DatabaseConfig} original class using Vert.x codegen.
 */
public class DatabaseConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, DatabaseConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "clusterKind":
          if (member.getValue() instanceof String) {
            obj.setClusterKind((String)member.getValue());
          }
          break;
        case "nodes":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<org.pharosnet.vertx.faas.database.core.config.NodeConfig> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                list.add(new org.pharosnet.vertx.faas.database.core.config.NodeConfig((io.vertx.core.json.JsonObject)item));
            });
            obj.setNodes(list);
          }
          break;
        case "transactionCacheMaxSize":
          if (member.getValue() instanceof Number) {
            obj.setTransactionCacheMaxSize(((Number)member.getValue()).longValue());
          }
          break;
        case "transactionCacheTTL":
          if (member.getValue() instanceof String) {
            obj.setTransactionCacheTTL((String)member.getValue());
          }
          break;
        case "type":
          if (member.getValue() instanceof String) {
            obj.setType((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(DatabaseConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(DatabaseConfig obj, java.util.Map<String, Object> json) {
    if (obj.getClusterKind() != null) {
      json.put("clusterKind", obj.getClusterKind());
    }
    if (obj.getNodes() != null) {
      JsonArray array = new JsonArray();
      obj.getNodes().forEach(item -> array.add(item.toJson()));
      json.put("nodes", array);
    }
    if (obj.getTransactionCacheMaxSize() != null) {
      json.put("transactionCacheMaxSize", obj.getTransactionCacheMaxSize());
    }
    if (obj.getTransactionCacheTTL() != null) {
      json.put("transactionCacheTTL", obj.getTransactionCacheTTL());
    }
    if (obj.getType() != null) {
      json.put("type", obj.getType());
    }
  }
}
