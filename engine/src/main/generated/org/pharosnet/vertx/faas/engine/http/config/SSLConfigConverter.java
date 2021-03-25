package org.pharosnet.vertx.faas.engine.http.config;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.vertx.faas.engine.http.config.SSLConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.vertx.faas.engine.http.config.SSLConfig} original class using Vert.x codegen.
 */
public class SSLConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, SSLConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "cert":
          if (member.getValue() instanceof String) {
            obj.setCert((String)member.getValue());
          }
          break;
        case "http2":
          if (member.getValue() instanceof Boolean) {
            obj.setHttp2((Boolean)member.getValue());
          }
          break;
        case "http2UseAlpn":
          if (member.getValue() instanceof Boolean) {
            obj.setHttp2UseAlpn((Boolean)member.getValue());
          }
          break;
        case "http2WindowSize":
          if (member.getValue() instanceof Number) {
            obj.setHttp2WindowSize(((Number)member.getValue()).intValue());
          }
          break;
        case "key":
          if (member.getValue() instanceof String) {
            obj.setKey((String)member.getValue());
          }
          break;
        case "keystore":
          if (member.getValue() instanceof String) {
            obj.setKeystore((String)member.getValue());
          }
          break;
        case "password":
          if (member.getValue() instanceof String) {
            obj.setPassword((String)member.getValue());
          }
          break;
        case "trust":
          if (member.getValue() instanceof Boolean) {
            obj.setTrust((Boolean)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(SSLConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(SSLConfig obj, java.util.Map<String, Object> json) {
    if (obj.getCert() != null) {
      json.put("cert", obj.getCert());
    }
    if (obj.getHttp2() != null) {
      json.put("http2", obj.getHttp2());
    }
    if (obj.getHttp2UseAlpn() != null) {
      json.put("http2UseAlpn", obj.getHttp2UseAlpn());
    }
    if (obj.getHttp2WindowSize() != null) {
      json.put("http2WindowSize", obj.getHttp2WindowSize());
    }
    if (obj.getKey() != null) {
      json.put("key", obj.getKey());
    }
    if (obj.getKeystore() != null) {
      json.put("keystore", obj.getKeystore());
    }
    if (obj.getPassword() != null) {
      json.put("password", obj.getPassword());
    }
    if (obj.getTrust() != null) {
      json.put("trust", obj.getTrust());
    }
  }
}
