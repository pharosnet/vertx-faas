package org.pharosnet.vertx.faas.engine.http;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.core.components.ComponentDeployment;
import org.pharosnet.vertx.faas.engine.http.router.AbstractHttpRouter;

public class HttpDeployment extends ComponentDeployment {

    public HttpDeployment(AbstractHttpRouter httpRouter, HttpServerHooker hooker) {
        super();
        this.httpRouter = httpRouter;
        this.hooker = hooker;
    }

    private final AbstractHttpRouter httpRouter;
    private final HttpServerHooker hooker;


    @Override
    public Future<String> deploy(Vertx vertx, JsonObject config) {
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setConfig(config);
        return vertx.deployVerticle(new HttpVerticle(httpRouter, hooker), deploymentOptions);
    }

}
