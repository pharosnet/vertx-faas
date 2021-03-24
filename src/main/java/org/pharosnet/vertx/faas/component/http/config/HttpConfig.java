package org.pharosnet.vertx.faas.component.http.config;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class HttpConfig {

    public static HttpConfig mapFrom(JsonObject jsonObject) {
        return new HttpConfig(jsonObject);
    }

    public HttpConfig() {
    }

    public HttpConfig(JsonObject jsonObject) {
        HttpConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        HttpConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private String host;

    private Integer port;

    private Boolean enableLogActivity;

    private Integer backlog;

    private CompressConfig compress;

    private SSLConfig ssl;

    private NetNativeConfig netNative;

    private JwtConfig jwt;

    private HttpDiscoveryConfig discovery;

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

    public Boolean getEnableLogActivity() {
        return enableLogActivity;
    }

    public void setEnableLogActivity(Boolean enableLogActivity) {
        this.enableLogActivity = enableLogActivity;
    }

    public Integer getBacklog() {
        return backlog;
    }

    public void setBacklog(Integer backlog) {
        this.backlog = backlog;
    }

    public CompressConfig getCompress() {
        return compress;
    }

    public void setCompress(CompressConfig compress) {
        this.compress = compress;
    }

    public SSLConfig getSsl() {
        return ssl;
    }

    public void setSsl(SSLConfig ssl) {
        this.ssl = ssl;
    }

    public NetNativeConfig getNetNative() {
        return netNative;
    }

    public void setNetNative(NetNativeConfig netNative) {
        this.netNative = netNative;
    }

    public HttpDiscoveryConfig getDiscovery() {
        return discovery;
    }

    public void setDiscovery(HttpDiscoveryConfig discovery) {
        this.discovery = discovery;
    }

    public JwtConfig getJwt() {
        return jwt;
    }

    public void setJwt(JwtConfig jwt) {
        this.jwt = jwt;
    }

}
