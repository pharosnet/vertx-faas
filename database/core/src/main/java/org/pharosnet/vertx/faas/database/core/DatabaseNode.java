package org.pharosnet.vertx.faas.database.core;

import io.vertx.core.Vertx;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import org.pharosnet.vertx.faas.database.core.config.NodeConfig;

import java.util.Optional;

public class DatabaseNode {

    public DatabaseNode(Vertx vertx, NodeConfig config, String type) {
        this.host = config.getHost();
        this.port = config.getPort();
        this.checkSQL = config.getCheckSQL();
        type = Optional.ofNullable(type).orElse("").trim().toUpperCase();
        if ("MYSQL".equals(type)) {
            this.createMysql(vertx, config);
        } else if ("POSTGRES".equals(type)) {
            this.createPostgres(vertx, config);
        } else {
            throw new RuntimeException("postgres config is invalid, type is invalid, it must be one of MYSQL or POSTGRES");
        }
    }

    private void createPostgres(Vertx vertx, NodeConfig config) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(config.getPort())
                .setHost(config.getHost())
                .setDatabase(config.getDatabase())
                .setUser(config.getUser())
                .setPassword(config.getPassword());

        if (Optional.ofNullable(config.getSsl()).orElse(false)) {
            connectOptions.setSsl(true);
            connectOptions.setSslMode(io.vertx.pgclient.SslMode.of(Optional.ofNullable(config.getSslMode()).orElse("disable").trim().toLowerCase()));
            connectOptions.setPemTrustOptions(new PemTrustOptions()
                    .addCertPath(config.getSslCertPath()));
        }

        int reconnectAttempts = Optional.ofNullable(config.getReconnectAttempts()).orElse(0);
        if (reconnectAttempts > 0) {
            connectOptions.setReconnectAttempts(reconnectAttempts);
        }

        int reconnectInterval = Optional.ofNullable(config.getReconnectInterval()).orElse(0);
        if (reconnectInterval > 0) {
            connectOptions.setReconnectInterval(reconnectInterval);
        }

        int maxPoolSize = Optional.ofNullable(config.getMaxPoolSize()).orElse(0);
        if (maxPoolSize < 1) {
            maxPoolSize = CpuCoreSensor.availableProcessors() * 2;
        }
        PoolOptions poolOptions = new PoolOptions().setMaxSize(maxPoolSize);

        this.pool = PgPool.pool(vertx, connectOptions, poolOptions);
    }

    private void createMysql(Vertx vertx, NodeConfig config) {
        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                .setPort(config.getPort())
                .setHost(config.getHost())
                .setDatabase(config.getDatabase())
                .setUser(config.getUser())
                .setPassword(config.getPassword());

        if (Optional.ofNullable(config.getSsl()).orElse(false)) {
            connectOptions.setSsl(true);
            connectOptions.setSslMode(io.vertx.mysqlclient.SslMode.of(Optional.ofNullable(config.getSslMode()).orElse("disable").trim().toLowerCase()));
            connectOptions.setPemTrustOptions(new PemTrustOptions()
                    .addCertPath(config.getSslCertPath()));
        }

        int reconnectAttempts = Optional.ofNullable(config.getReconnectAttempts()).orElse(0);
        if (reconnectAttempts > 0) {
            connectOptions.setReconnectAttempts(reconnectAttempts);
        }

        int reconnectInterval = Optional.ofNullable(config.getReconnectInterval()).orElse(0);
        if (reconnectInterval > 0) {
            connectOptions.setReconnectInterval(reconnectInterval);
        }

        String charset = Optional.ofNullable(config.getCharset()).orElse("");
        if (!charset.isBlank()) {
            connectOptions.setCharset(charset);
        }

        String collation = Optional.ofNullable(config.getCollation()).orElse("");
        if (!collation.isBlank()) {
            connectOptions.setCollation(collation);
        }

        int maxPoolSize = Optional.ofNullable(config.getMaxPoolSize()).orElse(0);
        if (maxPoolSize < 1) {
            maxPoolSize = CpuCoreSensor.availableProcessors() * 2;
        }
        PoolOptions poolOptions = new PoolOptions().setMaxSize(maxPoolSize);

        this.pool = MySQLPool.pool(vertx, connectOptions, poolOptions);
    }

    private String host;
    private Integer port;
    private Pool pool;
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

    public Pool getPool() {
        return pool;
    }

    public void setPool(Pool pool) {
        this.pool = pool;
    }

    public String getCheckSQL() {
        return checkSQL;
    }

    public void setCheckSQL(String checkSQL) {
        this.checkSQL = checkSQL;
    }
}
