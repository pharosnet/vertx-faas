package org.pharosnet.vertx.faas.database.core;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import org.pharosnet.vertx.faas.core.components.ComponentDeployment;
import org.pharosnet.vertx.faas.core.discovery.ServiceDiscoveryProvider;

import java.util.Optional;

public class DatabaseClusterDeployment extends ComponentDeployment {

    private DatabaseClusterDeployment() {
        this(0);
    }


    public DatabaseClusterDeployment(int workers) {
        super();
        if (workers < 0) {
            workers = CpuCoreSensor.availableProcessors() * 2;
        }
        this.workers = workers;
    }

    private final int workers;

    @Override
    public Future<String> deploy(Vertx vertx, JsonObject config) {
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        if (this.workers > 0) {
            deploymentOptions.setWorker(true).setWorkerPoolSize(this.workers);
        }
        deploymentOptions.setConfig(config);
        Optional<ServiceDiscovery> discovery = ServiceDiscoveryProvider.get();
        if (discovery.isEmpty()) {
            return Future.failedFuture("build clustered database failed, discovery is not defined.");
        }
        return vertx.deployVerticle(new DatabaseVerticle(discovery.get()), deploymentOptions);
    }

}
