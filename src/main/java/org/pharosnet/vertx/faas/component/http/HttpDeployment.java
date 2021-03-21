package org.pharosnet.vertx.faas.component.http;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.component.ComponentDeployment;
import org.pharosnet.vertx.faas.component.MessageConsumerRegister;
import org.pharosnet.vertx.faas.component.http.router.AbstractHttpRouter;
import org.pharosnet.vertx.faas.config.FaaSConfig;

public class HttpDeployment extends ComponentDeployment {

    public HttpDeployment(AbstractHttpRouter httpRouter, String basePackage) {
        super(null);
        this.httpRouter = httpRouter;
        this.basePackage = basePackage;
    }

    private final AbstractHttpRouter httpRouter;
    private final String basePackage;


    @Override
    public Future<String> deploy(Vertx vertx, FaaSConfig config) {
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setConfig(JsonObject.mapFrom(config.getHttp()));
        return vertx.deployVerticle(new HttpVerticle(super.getRegister(), httpRouter, basePackage), deploymentOptions);
    }

}
