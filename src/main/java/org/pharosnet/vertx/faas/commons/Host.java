package org.pharosnet.vertx.faas.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class Host {

    private static final Logger log = LoggerFactory.getLogger(Host.class);

    private static Host instance = null;

    public static void init() {
        String hostname = Optional.ofNullable(System.getenv("HOSTNAME")).orElse("").trim();
        if (hostname.length() > 0) {
            try {
                InetAddress addr = InetAddress.getByName(hostname);
                String ip = addr.getHostAddress();
                String hostName = addr.getHostName();
                instance = new Host(hostName, ip);
            } catch (UnknownHostException e) {
                log.error("无法获取本地 IP, HOSTNAME = {}, {}", hostname, e);
                System.exit(9);
            }
        } else {
            String appIp = Optional.ofNullable(System.getenv("APP_HTTP_IP")).orElse("").trim();
            if (appIp.length() > 0) {
                instance = new Host("localhost", appIp);
                return;
            }
            appIp = Optional.ofNullable(System.getProperty("app.http.ip")).orElse("").trim();
            if (appIp.length() > 0) {
                instance = new Host("localhost", appIp);
                return;
            }
            try {
                InetAddress addr = InetAddress.getLocalHost();
                instance = new Host(addr.getHostName(), addr.getHostAddress());
            } catch (UnknownHostException e) {
                log.error("无法获取HOSTNAME IP, {}", hostname, e);
                System.exit(9);
            }
        }
    }

    public static Host get() {
        if (instance == null) {
            init();
        }
        return instance;
    }

    public Host() {
    }

    public Host(String hostname, String ip) {
        this.hostname = hostname;
        this.ip = ip;
    }

    private String hostname;
    private String ip;

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }
}
