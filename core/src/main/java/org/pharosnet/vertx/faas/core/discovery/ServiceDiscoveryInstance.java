package org.pharosnet.vertx.faas.core.discovery;

import io.vertx.servicediscovery.ServiceDiscovery;

public class ServiceDiscoveryInstance {

    private static ServiceDiscoveryInstance instance;

    protected static ServiceDiscoveryInstance instance() {
        if (instance == null) {
            instance = new ServiceDiscoveryInstance();
        }
        return instance;
    }

    public static void set(ServiceDiscovery discovery) {
        instance = new ServiceDiscoveryInstance(discovery);
    }

    private ServiceDiscoveryInstance() {
        this.discovery = null;
    }

    protected ServiceDiscoveryInstance(ServiceDiscovery discovery) {
        this.discovery = discovery;
    }

    private final ServiceDiscovery discovery;

    protected ServiceDiscovery getDiscovery() {
        return discovery;
    }
}
