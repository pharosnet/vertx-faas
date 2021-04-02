package org.pharosnet.vertx.faas.database.core.config;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.vertx.faas.database.core.config.NodeConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.vertx.faas.database.core.config.NodeConfig} original class using Vert.x codegen.
 */
public class NodeConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, NodeConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "charset":
          if (member.getValue() instanceof String) {
            obj.setCharset((String)member.getValue());
          }
          break;
        case "checkSQL":
          if (member.getValue() instanceof String) {
            obj.setCheckSQL((String)member.getValue());
          }
          break;
        case "collation":
          if (member.getValue() instanceof String) {
            obj.setCollation((String)member.getValue());
          }
          break;
        case "database":
          if (member.getValue() instanceof String) {
            obj.setDatabase((String)member.getValue());
          }
          break;
        case "host":
          if (member.getValue() instanceof String) {
            obj.setHost((String)member.getValue());
          }
          break;
        case "maxPoolSize":
          if (member.getValue() instanceof Number) {
            obj.setMaxPoolSize(((Number)member.getValue()).intValue());
          }
          break;
        case "password":
          if (member.getValue() instanceof String) {
            obj.setPassword((String)member.getValue());
          }
          break;
        case "port":
          if (member.getValue() instanceof Number) {
            obj.setPort(((Number)member.getValue()).intValue());
          }
          break;
        case "reconnectAttempts":
          if (member.getValue() instanceof Number) {
            obj.setReconnectAttempts(((Number)member.getValue()).intValue());
          }
          break;
        case "reconnectInterval":
          if (member.getValue() instanceof Number) {
            obj.setReconnectInterval(((Number)member.getValue()).intValue());
          }
          break;
        case "ssl":
          if (member.getValue() instanceof Boolean) {
            obj.setSsl((Boolean)member.getValue());
          }
          break;
        case "sslCertPath":
          if (member.getValue() instanceof String) {
            obj.setSslCertPath((String)member.getValue());
          }
          break;
        case "sslMode":
          if (member.getValue() instanceof String) {
            obj.setSslMode((String)member.getValue());
          }
          break;
        case "user":
          if (member.getValue() instanceof String) {
            obj.setUser((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(NodeConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(NodeConfig obj, java.util.Map<String, Object> json) {
    if (obj.getCharset() != null) {
      json.put("charset", obj.getCharset());
    }
    if (obj.getCheckSQL() != null) {
      json.put("checkSQL", obj.getCheckSQL());
    }
    if (obj.getCollation() != null) {
      json.put("collation", obj.getCollation());
    }
    if (obj.getDatabase() != null) {
      json.put("database", obj.getDatabase());
    }
    if (obj.getHost() != null) {
      json.put("host", obj.getHost());
    }
    if (obj.getMaxPoolSize() != null) {
      json.put("maxPoolSize", obj.getMaxPoolSize());
    }
    if (obj.getPassword() != null) {
      json.put("password", obj.getPassword());
    }
    if (obj.getPort() != null) {
      json.put("port", obj.getPort());
    }
    if (obj.getReconnectAttempts() != null) {
      json.put("reconnectAttempts", obj.getReconnectAttempts());
    }
    if (obj.getReconnectInterval() != null) {
      json.put("reconnectInterval", obj.getReconnectInterval());
    }
    if (obj.getSsl() != null) {
      json.put("ssl", obj.getSsl());
    }
    if (obj.getSslCertPath() != null) {
      json.put("sslCertPath", obj.getSslCertPath());
    }
    if (obj.getSslMode() != null) {
      json.put("sslMode", obj.getSslMode());
    }
    if (obj.getUser() != null) {
      json.put("user", obj.getUser());
    }
  }
}
