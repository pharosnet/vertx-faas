package org.pharosnet.vertx.faas.engine.http.config;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.vertx.faas.engine.http.config.CompressConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.vertx.faas.engine.http.config.CompressConfig} original class using Vert.x codegen.
 */
public class CompressConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, CompressConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "compression":
          if (member.getValue() instanceof Boolean) {
            obj.setCompression((Boolean)member.getValue());
          }
          break;
        case "decompression":
          if (member.getValue() instanceof Boolean) {
            obj.setDecompression((Boolean)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(CompressConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(CompressConfig obj, java.util.Map<String, Object> json) {
    if (obj.getCompression() != null) {
      json.put("compression", obj.getCompression());
    }
    if (obj.getDecompression() != null) {
      json.put("decompression", obj.getDecompression());
    }
  }
}
