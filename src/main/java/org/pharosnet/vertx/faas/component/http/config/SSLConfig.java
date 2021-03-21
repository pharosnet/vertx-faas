package org.pharosnet.vertx.faas.component.http.config;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class SSLConfig {

    public SSLConfig() {
    }

    public SSLConfig(JsonObject jsonObject) {
        SSLConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        SSLConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private String keystore;
    private String password;
    private Boolean trust;

    private String cert;
    private String key;

    private Boolean http2;
    private Boolean http2UseAlpn;
    private Integer http2WindowSize;

    public String getKeystore() {
        return keystore;
    }

    public void setKeystore(String keystore) {
        this.keystore = keystore;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getTrust() {
        return trust;
    }

    public void setTrust(Boolean trust) {
        this.trust = trust;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Boolean getHttp2() {
        return http2;
    }

    public void setHttp2(Boolean http2) {
        this.http2 = http2;
    }

    public Boolean getHttp2UseAlpn() {
        return http2UseAlpn;
    }

    public void setHttp2UseAlpn(Boolean http2UseAlpn) {
        this.http2UseAlpn = http2UseAlpn;
    }

    public Integer getHttp2WindowSize() {
        return http2WindowSize;
    }

    public void setHttp2WindowSize(Integer http2WindowSize) {
        this.http2WindowSize = http2WindowSize;
    }
}
