package org.pharosnet.vertx.faas.engine.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.core.commons.FaaSActive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Optional;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private static final String PATH_KEY = "FAAS_CONFIG_PATH";
    private static final String KIND_KEY = "FAAS_CONFIG_KIND";
    private static final String FORMAT_KEY = "FAAS_CONFIG_FORMAT";

    public static Future<JsonObject> read(Vertx vertx) {

        String kind = Optional.ofNullable(System.getenv(KIND_KEY)).orElse("").trim();
        if (kind.isBlank()) {
            kind = "FILE";
        }
        String format = Optional.ofNullable(System.getenv(FORMAT_KEY)).orElse("").trim().toLowerCase();
        if (format.isBlank()) {
            format = "json";
        }

        if (kind.equalsIgnoreCase("FILE")) {
            return readFromFile(vertx, format);
        }

        return Future.failedFuture("读取配置文件失败，未知FAAS_CONFIG_KIND。");
    }

    private static Future<JsonObject> readFromFile(Vertx vertx, String format) {
        Promise<JsonObject> promise = Promise.promise();
        String filename = String.format("faas-%s.%s", FaaSActive.get().getValue(), format);
        URL configURL = Thread.currentThread().getContextClassLoader().getResource(filename);
        if (configURL == null) {
            log.error("读取配置文件错误，无法找到文件。{}", filename);
            promise.fail("读取配置文件错误，无法找到文件。");
            return promise.future();
        }
        String configFile = configURL.getFile();
        ConfigStoreOptions configStoreOptions = new ConfigStoreOptions()
                .setType("file")
                .setFormat(format)
                .setConfig(new JsonObject().put("path", configFile));
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(configStoreOptions));
        retriever.getConfig().onSuccess(r -> {
            if (log.isDebugEnabled()) {
                log.debug("config : \n {}", r.encodePrettily());
            }
            promise.complete(r);
        }).onFailure(e -> {
            log.error("读取配置文件错误, {}", configFile, e);
            promise.fail("读取配置文件错误");
        });
        return promise.future();
    }

}
