package org.pharosnet.vertx.faas.engine.http.auth.jwt;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.List;

@DataObject(generateConverter = true)
public class JwtConfig {

    public JwtConfig() {
    }

    public JwtConfig(JsonObject jsonObject) {
        JwtConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        JwtConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    // RS256 HS256
    private String algorithm;

    private String key;
    private String priKey;
    private String pubKey;

    private String expirationDuration;
    private Boolean expirationIgnored;
    private String issuer;
    private List<String> audience;
    private Integer leeway;

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPriKey() {
        return priKey;
    }

    public void setPriKey(String priKey) {
        this.priKey = priKey;
    }

    public String getPubKey() {
        return pubKey;
    }

    public void setPubKey(String pubKey) {
        this.pubKey = pubKey;
    }

    public String getExpirationDuration() {
        return expirationDuration;
    }

    public void setExpirationDuration(String expirationDuration) {
        this.expirationDuration = expirationDuration;
    }

    public Boolean getExpirationIgnored() {
        return expirationIgnored;
    }

    public void setExpirationIgnored(Boolean expirationIgnored) {
        this.expirationIgnored = expirationIgnored;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public List<String> getAudience() {
        return audience;
    }

    public void setAudience(List<String> audience) {
        this.audience = audience;
    }

    public Integer getLeeway() {
        return leeway;
    }

    public void setLeeway(Integer leeway) {
        this.leeway = leeway;
    }
}
