package org.pharosnet.vertx.faas.codegen.generators;

import com.squareup.javapoet.*;
import io.vertx.codegen.format.CamelCase;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.codegen.annotation.FnModule;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

public class DeploymentGenerator {

    public DeploymentGenerator(Messager messager, Elements elementUtils, Filer filer) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.filer = filer;
    }

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;

    public void generate(String pkg, FnModule fnModule) throws Exception {
        List<String> verticleName = new ArrayList<>(List.of(fnModule.name().split("-")));
        verticleName.add("verticle");
        List<String> deploymentName = new ArrayList<>(List.of(fnModule.name().split("-")));
        deploymentName.add("deployment");

        String fnDeploymentClassName = CamelCase.INSTANCE.format(deploymentName);
        String fnVerticleClassName = CamelCase.INSTANCE.format(verticleName);


        // construct
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super()");


        // deploy()
        MethodSpec.Builder deployMethod = MethodSpec.methodBuilder("deploy")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Vertx.class), "vertx")
                .addParameter(ClassName.get(JsonObject.class), "config")
                .returns(ParameterizedTypeName.get(ClassName.get(Future.class), ClassName.get(String.class)));

        deployMethod.addStatement("$T deploymentOptions = new $T()", ClassName.get(DeploymentOptions.class), ClassName.get(DeploymentOptions.class));
        deployMethod.addStatement("deploymentOptions.setConfig(config)");
        if (fnModule.instances() > 1) {
            deployMethod.addStatement(String.format("deploymentOptions.setInstances(%d)", fnModule.instances()));
        }
        if (fnModule.workers() > 0) {
            deployMethod.addStatement(String.format("deploymentOptions.setWorker(true).setWorkerPoolSize(%d)", fnModule.workers()));
        }
        deployMethod.addStatement("return vertx.deployVerticle($T.class, deploymentOptions)", ClassName.get(pkg, fnVerticleClassName));


        // class
        TypeSpec typeBuilder = TypeSpec.classBuilder(fnDeploymentClassName)
                .addAnnotation(ClassName.get("org.pharosnet.vertx.faas.core.annotations", "FnDeployment"))
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get("org.pharosnet.vertx.faas.core.components", "ComponentDeployment"))
                .addMethod(constructor.build())
                .addMethod(deployMethod.build())
                .build();

        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder)
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
                .indent("\t")
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, fnDeploymentClassName));
    }
}
