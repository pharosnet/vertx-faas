package org.pharosnet.vertx.faas.component.http.config;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.vertx.faas.component.http.config.HttpDiscoveryConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.vertx.faas.component.http.config.HttpDiscoveryConfig} original class using Vert.x codegen.
 */
public class HttpDiscoveryConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, HttpDiscoveryConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "config":
          if (member.getValue() instanceof JsonObject) {
            obj.setConfig(((JsonObject)member.getValue()).copy());
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

  public static void toJson(HttpDiscoveryConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(HttpDiscoveryConfig obj, java.util.Map<String, Object> json) {
    if (obj.getConfig() != null) {
      json.put("config", obj.getConfig());
    }
    if (obj.getType() != null) {
      json.put("type", obj.getType());
    }
  }
}
