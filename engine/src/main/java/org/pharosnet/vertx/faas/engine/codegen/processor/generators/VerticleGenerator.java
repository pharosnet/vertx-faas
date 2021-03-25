package org.pharosnet.vertx.faas.engine.codegen.processor.generators;

import com.squareup.javapoet.*;
import io.vertx.codegen.annotations.ModuleGen;
import io.vertx.codegen.format.CamelCase;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.core.components.MessageConsumerRegister;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.stream.Collectors;

public class VerticleGenerator {

    public VerticleGenerator(Messager messager, Elements elementUtils, Filer filer) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.filer = filer;
    }

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;

    public void generate(String pkg, ModuleGen moduleGen) throws Exception {
        String fnVerticleClassName = CamelCase.INSTANCE.format(List.of(moduleGen.name(), "verticle"));

        // construct
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(MessageConsumerRegister.class), "register")
                .addStatement("this.register = register");

        // register
        FieldSpec.Builder registerField = FieldSpec.builder(
                ClassName.get(MessageConsumerRegister.class), "register",
                Modifier.PRIVATE, Modifier.FINAL);

        // consumers
        FieldSpec.Builder consumersField = FieldSpec.builder(
                ParameterizedTypeName.get(
                        ClassName.get(List.class),
                        ParameterizedTypeName.get(
                                ClassName.get(MessageConsumer.class),
                                ClassName.get(JsonObject.class)
                        )
                ), "consumers",
                Modifier.PRIVATE);

        // register()
        MethodSpec.Builder registerMethod = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PRIVATE)
                .addStatement("this.consumers = this.register.register(this.vertx)")
                .returns(ClassName.VOID);

        // unregister()
        MethodSpec.Builder unregisterMethod = MethodSpec.methodBuilder("unregister")
                .addModifiers(Modifier.PRIVATE)
                .addStatement("$T promise = $T.promise()",
                        ParameterizedTypeName.get(ClassName.get(Promise.class), ClassName.get(Void.class)),
                        ClassName.get(Promise.class))
                .addCode("if (consumers == null) {\n")
                .addCode("\tpromise.complete();\n")
                .addCode("\treturn promise.future();\n")
                .addCode("}\n")
                .addCode("$T compositeFuture = $T.all(consumers.stream().map(consumer -> {\n",
                        ClassName.get(CompositeFuture.class), ClassName.get(CompositeFuture.class))
                .addCode("\t$T unregisterPromise = $T.promise();\n",
                        ParameterizedTypeName.get(ClassName.get(Promise.class), ClassName.get(Void.class)),
                        ClassName.get(Promise.class))
                .addCode("\tconsumer.unregister(unregisterPromise);\n")
                .addCode("\treturn unregisterPromise.future();\n")
                .addCode("}).collect($T.toList()));\n", ClassName.get(Collectors.class))
                .addCode("\n")
                .addCode("compositeFuture.onSuccess(r -> promise.complete());\n")
                .addCode("compositeFuture.onFailure(promise::fail);\n")
                .addCode("return promise.future();")
                .returns(ParameterizedTypeName.get(ClassName.get(Future.class), ClassName.get(Void.class)));

        // start()
        MethodSpec.Builder startMethod = MethodSpec.methodBuilder("start")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Promise.class), ClassName.get(Void.class)), "promise")
                .addStatement("this.register()")
                .addStatement("promise.complete()")
                .addException(ClassName.get(Exception.class))
                .returns(ClassName.VOID);

        // stop()
        MethodSpec.Builder stopMethod = MethodSpec.methodBuilder("stop")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Promise.class), ClassName.get(Void.class)), "promise")
                .addCode("this.unregister()\n")
                .addCode("\t\t.onSuccess(r -> promise.complete())\n")
                .addCode("\t\t.onFailure(e -> promise.fail(e.getMessage()));\n")
                .addException(ClassName.get(Exception.class))
                .returns(ClassName.VOID);

        // class
        TypeSpec typeBuilder = TypeSpec.classBuilder(fnVerticleClassName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(AbstractVerticle.class))
                .addMethod(constructor.build())
                .addField(registerField.build())
                .addField(consumersField.build())
                .addMethod(registerMethod.build())
                .addMethod(unregisterMethod.build())
                .addMethod(startMethod.build())
                .addMethod(stopMethod.build())
                .build();

        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder)
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
                .indent("\t")
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, fnVerticleClassName));
    }
}
