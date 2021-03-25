package org.pharosnet.vertx.faas.core.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class Host {

    private static final Logger log = LoggerFactory.getLogger(Host.class);

    private static final String IP_KEY = "FAAS_IP";

    public static Host get() throws UnknownHostException {
        String ip = Optional.ofNullable(System.getenv(IP_KEY)).orElse("").trim();
        if (FaaSActive.get().equals(FaaSActive.LOCAL)) {
            if (ip.isBlank()) {
                ip = "127.0.0.1";
            }
            return new Host("localhost", ip);
        }
        String hostname = Optional.ofNullable(System.getenv("HOSTNAME")).orElse("").trim();
        if (hostname.isBlank()) {
            if (ip.isBlank()) {
                throw new UnknownHostException("无法获取到HOSTNAME， 且FAAS_IP也为指定。");
            }
            return new Host("localhost", ip);
        }

        try {
            InetAddress addr = InetAddress.getByName(hostname);
            String hostAddress = addr.getHostAddress();
            String hostName = addr.getHostName();
            if (!ip.isBlank()) {
                new Host(hostName, ip);
            }
            return new Host(hostName, hostAddress);
        } catch (UnknownHostException e) {
            log.error("无法从HOSTNAME({})获取IP。", hostname, e);
            throw new UnknownHostException(String.format("无法从HOSTNAME(%s)获取IP。%s", hostname, e.getMessage()));
        }
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
