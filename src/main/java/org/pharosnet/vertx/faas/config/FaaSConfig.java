package org.pharosnet.vertx.faas.config;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.commons.AppLevel;
import org.pharosnet.vertx.faas.component.http.config.HttpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;

@DataObject(generateConverter = true)
public class FaaSConfig {

    private static final Logger log = LoggerFactory.getLogger(FaaSConfig.class);

    public static Future<FaaSConfig> read(Vertx vertx) {
        Promise<FaaSConfig> promise = Promise.promise();
        String filename = String.format("config-%s.json", AppLevel.get().getValue());
        URL configURL = Thread.currentThread().getContextClassLoader().getResource(filename);
        if (configURL == null) {
            log.error("读取配置文件错误，无法找到文件。{}", filename);
            promise.fail("读取配置文件错误，无法找到文件。");
            return promise.future();
        }
        String configFile = configURL.getFile();
        ConfigStoreOptions configStoreOptions = new ConfigStoreOptions()
                .setType("file")
                .setFormat("json")
                .setConfig(new JsonObject().put("path", configFile));
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(configStoreOptions));
        retriever.getConfig().onSuccess(r -> {
            promise.complete(new FaaSConfig(r));
        }).onFailure(e -> {
            log.error("读取配置文件错误, {}", configFile, e);
            promise.fail("读取配置文件错误");
        });
        return promise.future();
    }

    public static FaaSConfig mapFrom(JsonObject jsonObject) {
        return new FaaSConfig(jsonObject);
    }

    public FaaSConfig() {
    }

    public FaaSConfig(JsonObject jsonObject) {
        FaaSConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        FaaSConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private HttpConfig http;

    private Map<String, JsonObject> options;

    public <T> T getFromOptions(String key, Class<T> clazz) {
        if (this.options == null || this.options.isEmpty()) {
            return null;
        }
        if (!this.options.containsKey(key)) {
            return null;
        }
        JsonObject option = this.options.get(key);
        if (option == null) {
            return null;
        }
        return option.mapTo(clazz);
    }

    public HttpConfig getHttp() {
        return http;
    }

    public void setHttp(HttpConfig http) {
        this.http = http;
    }

    public Map<String, JsonObject> getOptions() {
        return options;
    }

    public void setOptions(Map<String, JsonObject> options) {
        this.options = options;
    }

}
