package org.pharosnet.vertx.faas.engine.http.config;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class NetNativeConfig {

    public NetNativeConfig() {
    }

    public NetNativeConfig(JsonObject jsonObject) {
        NetNativeConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        NetNativeConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }


    private Boolean tcpFastOpen;
    private Boolean tcpCork;
    private Boolean tcpQuickAck;
    private Boolean reusePort;

    public Boolean getTcpFastOpen() {
        return tcpFastOpen;
    }

    public void setTcpFastOpen(Boolean tcpFastOpen) {
        this.tcpFastOpen = tcpFastOpen;
    }

    public Boolean getTcpCork() {
        return tcpCork;
    }

    public void setTcpCork(Boolean tcpCork) {
        this.tcpCork = tcpCork;
    }

    public Boolean getTcpQuickAck() {
        return tcpQuickAck;
    }

    public void setTcpQuickAck(Boolean tcpQuickAck) {
        this.tcpQuickAck = tcpQuickAck;
    }

    public Boolean getReusePort() {
        return reusePort;
    }

    public void setReusePort(Boolean reusePort) {
        this.reusePort = reusePort;
    }

}
