package org.pharosnet.vertx.faas.database.core.config;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class NodeConfig {

    public NodeConfig() {
    }

    public NodeConfig(JsonObject jsonObject) {
        NodeConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        NodeConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private String host;
    private Integer port;
    private String database;
    private String user;
    private String password;
    private String charset;
    private String collation;
    private Integer reconnectAttempts;
    private Integer reconnectInterval;

    private Integer maxPoolSize;

    private Boolean ssl;
    private String sslMode;
    private String sslCertPath;

    private String checkSQL;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCollation() {
        return collation;
    }

    public void setCollation(String collation) {
        this.collation = collation;
    }

    public Integer getReconnectAttempts() {
        return reconnectAttempts;
    }

    public void setReconnectAttempts(Integer reconnectAttempts) {
        this.reconnectAttempts = reconnectAttempts;
    }

    public Integer getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(Integer reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Boolean getSsl() {
        return ssl;
    }

    public void setSsl(Boolean ssl) {
        this.ssl = ssl;
    }

    public String getSslMode() {
        return sslMode;
    }

    public void setSslMode(String sslMode) {
        this.sslMode = sslMode;
    }

    public String getSslCertPath() {
        return sslCertPath;
    }

    public void setSslCertPath(String sslCertPath) {
        this.sslCertPath = sslCertPath;
    }

    public String getCheckSQL() {
        return checkSQL;
    }

    public void setCheckSQL(String checkSQL) {
        this.checkSQL = checkSQL;
    }
}

