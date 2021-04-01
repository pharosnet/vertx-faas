package org.pharosnet.vertx.faas.engine.http;

public class HttpInfo {

    public HttpInfo(String host, Integer port, String name, String rootPath) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.rootPath = rootPath;
    }

    private String host;
    private Integer port;
    private String name;
    private String rootPath;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
}
