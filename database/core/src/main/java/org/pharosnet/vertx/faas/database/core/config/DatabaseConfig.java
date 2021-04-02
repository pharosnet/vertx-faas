package org.pharosnet.vertx.faas.database.core.config;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.List;

@DataObject(generateConverter = true)
public class DatabaseConfig {

    public DatabaseConfig() {
    }

    public DatabaseConfig(JsonObject jsonObject) {
        DatabaseConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        DatabaseConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    // postgres, mysql
    private String type;
    // master_slaver, xl
    private String clusterKind;

    // [0] = standalone or master, [1,n] = slavers, [0, n] = xl
    private List<NodeConfig> nodes;

    private String transactionCacheTTL;
    private Long transactionCacheMaxSize;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClusterKind() {
        return clusterKind;
    }

    public void setClusterKind(String clusterKind) {
        this.clusterKind = clusterKind;
    }

    public List<NodeConfig> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeConfig> nodes) {
        this.nodes = nodes;
    }

    public String getTransactionCacheTTL() {
        return transactionCacheTTL;
    }

    public void setTransactionCacheTTL(String transactionCacheTTL) {
        this.transactionCacheTTL = transactionCacheTTL;
    }

    public Long getTransactionCacheMaxSize() {
        return transactionCacheMaxSize;
    }

    public void setTransactionCacheMaxSize(Long transactionCacheMaxSize) {
        this.transactionCacheMaxSize = transactionCacheMaxSize;
    }

}
