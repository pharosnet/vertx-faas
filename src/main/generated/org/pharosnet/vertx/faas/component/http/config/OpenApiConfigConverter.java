package org.pharosnet.vertx.faas.component.http.config;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.vertx.faas.component.http.config.OpenApiConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.vertx.faas.component.http.config.OpenApiConfig} original class using Vert.x codegen.
 */
public class OpenApiConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, OpenApiConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "jsonPath":
          if (member.getValue() instanceof String) {
            obj.setJsonPath((String)member.getValue());
          }
          break;
        case "webPathPrefix":
          if (member.getValue() instanceof String) {
            obj.setWebPathPrefix((String)member.getValue());
          }
          break;
        case "webStaticResourcePath":
          if (member.getValue() instanceof String) {
            obj.setWebStaticResourcePath((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(OpenApiConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(OpenApiConfig obj, java.util.Map<String, Object> json) {
    if (obj.getJsonPath() != null) {
      json.put("jsonPath", obj.getJsonPath());
    }
    if (obj.getWebPathPrefix() != null) {
      json.put("webPathPrefix", obj.getWebPathPrefix());
    }
    if (obj.getWebStaticResourcePath() != null) {
      json.put("webStaticResourcePath", obj.getWebStaticResourcePath());
    }
  }
}
