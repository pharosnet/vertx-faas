package org.pharosnet.vertx.faas.core.discovery;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;

import java.util.Optional;

public interface ServiceDiscoveryProvider {

    static Optional<ServiceDiscovery> get() {
        return Optional.ofNullable(ServiceDiscoveryInstance.instance().getDiscovery());
    }

    ServiceDiscovery create(Vertx vertx, JsonObject config) throws Exception;

}
