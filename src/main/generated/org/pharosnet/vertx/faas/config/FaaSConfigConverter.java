package org.pharosnet.vertx.faas.config;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.vertx.faas.config.FaaSConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.vertx.faas.config.FaaSConfig} original class using Vert.x codegen.
 */
public class FaaSConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, FaaSConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "http":
          if (member.getValue() instanceof JsonObject) {
            obj.setHttp(new org.pharosnet.vertx.faas.component.http.config.HttpConfig((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "options":
          if (member.getValue() instanceof JsonObject) {
            java.util.Map<String, io.vertx.core.json.JsonObject> map = new java.util.LinkedHashMap<>();
            ((Iterable<java.util.Map.Entry<String, Object>>)member.getValue()).forEach(entry -> {
              if (entry.getValue() instanceof JsonObject)
                map.put(entry.getKey(), ((JsonObject)entry.getValue()).copy());
            });
            obj.setOptions(map);
          }
          break;
      }
    }
  }

  public static void toJson(FaaSConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(FaaSConfig obj, java.util.Map<String, Object> json) {
    if (obj.getHttp() != null) {
      json.put("http", obj.getHttp().toJson());
    }
    if (obj.getOptions() != null) {
      JsonObject map = new JsonObject();
      obj.getOptions().forEach((key, value) -> map.put(key, value));
      json.put("options", map);
    }
  }
}
