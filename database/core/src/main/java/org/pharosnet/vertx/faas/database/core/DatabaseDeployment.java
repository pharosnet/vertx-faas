package org.pharosnet.vertx.faas.database.core;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.core.components.ComponentDeployment;

public class DatabaseDeployment extends ComponentDeployment {

    public DatabaseDeployment() {
        this(0);
    }

    public DatabaseDeployment(int workers) {
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
        return vertx.deployVerticle(new DatabaseVerticle(), deploymentOptions);
    }

}
