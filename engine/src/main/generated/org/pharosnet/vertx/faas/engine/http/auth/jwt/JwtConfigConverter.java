package org.pharosnet.vertx.faas.engine.http.auth.jwt;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.vertx.faas.engine.http.auth.jwt.JwtConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.vertx.faas.engine.http.auth.jwt.JwtConfig} original class using Vert.x codegen.
 */
public class JwtConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, JwtConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "algorithm":
          if (member.getValue() instanceof String) {
            obj.setAlgorithm((String)member.getValue());
          }
          break;
        case "audience":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<java.lang.String> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                list.add((String)item);
            });
            obj.setAudience(list);
          }
          break;
        case "expirationDuration":
          if (member.getValue() instanceof String) {
            obj.setExpirationDuration((String)member.getValue());
          }
          break;
        case "expirationIgnored":
          if (member.getValue() instanceof Boolean) {
            obj.setExpirationIgnored((Boolean)member.getValue());
          }
          break;
        case "issuer":
          if (member.getValue() instanceof String) {
            obj.setIssuer((String)member.getValue());
          }
          break;
        case "key":
          if (member.getValue() instanceof String) {
            obj.setKey((String)member.getValue());
          }
          break;
        case "leeway":
          if (member.getValue() instanceof Number) {
            obj.setLeeway(((Number)member.getValue()).intValue());
          }
          break;
        case "priKey":
          if (member.getValue() instanceof String) {
            obj.setPriKey((String)member.getValue());
          }
          break;
        case "pubKey":
          if (member.getValue() instanceof String) {
            obj.setPubKey((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(JwtConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(JwtConfig obj, java.util.Map<String, Object> json) {
    if (obj.getAlgorithm() != null) {
      json.put("algorithm", obj.getAlgorithm());
    }
    if (obj.getAudience() != null) {
      JsonArray array = new JsonArray();
      obj.getAudience().forEach(item -> array.add(item));
      json.put("audience", array);
    }
    if (obj.getExpirationDuration() != null) {
      json.put("expirationDuration", obj.getExpirationDuration());
    }
    if (obj.getExpirationIgnored() != null) {
      json.put("expirationIgnored", obj.getExpirationIgnored());
    }
    if (obj.getIssuer() != null) {
      json.put("issuer", obj.getIssuer());
    }
    if (obj.getKey() != null) {
      json.put("key", obj.getKey());
    }
    if (obj.getLeeway() != null) {
      json.put("leeway", obj.getLeeway());
    }
    if (obj.getPriKey() != null) {
      json.put("priKey", obj.getPriKey());
    }
    if (obj.getPubKey() != null) {
      json.put("pubKey", obj.getPubKey());
    }
  }
}
