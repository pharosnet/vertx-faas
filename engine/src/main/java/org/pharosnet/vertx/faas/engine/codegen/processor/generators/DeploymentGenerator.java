package org.pharosnet.vertx.faas.engine.codegen.processor.generators;

import com.squareup.javapoet.*;
import io.vertx.codegen.annotations.ModuleGen;
import io.vertx.codegen.format.CamelCase;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.core.components.ComponentDeployment;
import org.pharosnet.vertx.faas.core.components.MessageConsumerRegister;
import org.pharosnet.vertx.faas.engine.codegen.annotation.FnDeployment;
import org.pharosnet.vertx.faas.engine.config.Config;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
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

    public void generate(String pkg, ModuleGen moduleGen) throws Exception {
        String fnDeploymentClassName = CamelCase.INSTANCE.format(List.of(moduleGen.name(), "deployment"));
        String fnVerticleClassName = CamelCase.INSTANCE.format(List.of(moduleGen.name(), "verticle"));

        String fnMessageConsumerRegisterClassName = String.format("%sMessageConsumerRegister", CamelCase.INSTANCE.format(List.of(moduleGen.name())));

        // construct
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super()")
                .addStatement("super.setRegister(new $T())", ClassName.get(pkg, fnMessageConsumerRegisterClassName));

        // constructorByRegister
        MethodSpec.Builder constructorByRegister = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(MessageConsumerRegister.class), "register")
                .addStatement("super(register)");


        // deploy()
        MethodSpec.Builder deployMethod = MethodSpec.methodBuilder("deploy")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Vertx.class), "vertx")
                .addParameter(ClassName.get(JsonObject.class), "config")
                .addStatement("$T deploymentOptions = new $T()", ClassName.get(DeploymentOptions.class), ClassName.get(DeploymentOptions.class))
                .addStatement("deploymentOptions.setConfig(config)")
                .addStatement("return vertx.deployVerticle(new $T(super.getRegister()), deploymentOptions)", ClassName.get(pkg, fnVerticleClassName))
                .returns(ParameterizedTypeName.get(ClassName.get(Future.class), ClassName.get(String.class)));


        // class
        TypeSpec typeBuilder = TypeSpec.classBuilder(fnDeploymentClassName)
                .addAnnotation(FnDeployment.class)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(ComponentDeployment.class))
                .addMethod(constructor.build())
                .addMethod(constructorByRegister.build())
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
