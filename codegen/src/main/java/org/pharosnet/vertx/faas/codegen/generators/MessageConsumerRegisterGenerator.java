package org.pharosnet.vertx.faas.codegen.generators;

import com.squareup.javapoet.*;
import io.vertx.codegen.format.CamelCase;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.codegen.annotation.FnModule;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessageConsumerRegisterGenerator {

    public MessageConsumerRegisterGenerator(Messager messager, Elements elementUtils, Filer filer) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.filer = filer;
    }

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;

    public void generate(String pkg, FnModule fnModule, List<FnImpl> fnImpls) throws Exception {
        List<String> nameItems = new ArrayList<>(List.of(fnModule.name().split("-")));

        String fnMessageConsumerRegisterClassName = String.format("%sMessageConsumerRegister", CamelCase.INSTANCE.format(nameItems));

        // consumers
        FieldSpec.Builder consumersField = FieldSpec.builder(
                ParameterizedTypeName.get(
                        ClassName.get(List.class),
                        ParameterizedTypeName.get(
                                ClassName.get(MessageConsumer.class),
                                ClassName.get(JsonObject.class)
                        )), "consumers",
                Modifier.PRIVATE);

        // register()
        MethodSpec.Builder registerMethod = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Vertx.class), "vertx")
                .addStatement("$T consumers = new $T()",
                        ParameterizedTypeName.get(
                                ClassName.get(List.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(MessageConsumer.class),
                                        ClassName.get(JsonObject.class)
                                )), ClassName.get(ArrayList.class));

        for (FnImpl fnImpl : fnImpls) {
            String fnServiceImplClassName = String.format("%sServiceImpl", fnImpl.getInterfaceTypeElement().getSimpleName());
            registerMethod.addStatement("consumers.add($T.register(vertx));", ClassName.get(pkg, fnServiceImplClassName));
        }
        registerMethod.addStatement("this.consumers = consumers");

        registerMethod.returns(ClassName.VOID);


        // unregister()
        MethodSpec.Builder unregisterMethod = MethodSpec.methodBuilder("unregister")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(
                        ClassName.get(Future.class),
                        ClassName.get(Void.class)
                ));
        unregisterMethod.addStatement("$T promise = $T.promise()",
                ParameterizedTypeName.get(
                        ClassName.get(Promise.class),
                        ClassName.get(Void.class)
                ),
                ClassName.get(Promise.class));
        unregisterMethod.addCode("if (consumers == null) {\n");
        unregisterMethod.addCode("\tpromise.complete();\n");
        unregisterMethod.addCode("\treturn promise.future();\n");
        unregisterMethod.addCode("}\n");
        unregisterMethod.addCode("$T compositeFuture = $T.all(consumers.stream().map(consumer -> {\n",
                ClassName.get(CompositeFuture.class), ClassName.get(CompositeFuture.class));
        unregisterMethod.addCode("\t$T unregisterPromise = $T.promise();\n",
                ParameterizedTypeName.get(
                        ClassName.get(Promise.class),
                        ClassName.get(Void.class)
                ),
                ClassName.get(Promise.class));
        unregisterMethod.addCode("\tconsumer.unregister(unregisterPromise);\n");
        unregisterMethod.addCode("\treturn unregisterPromise.future();\n");
        unregisterMethod.addCode("}).collect($T.toList()));\n\n", ClassName.get(Collectors.class));
        unregisterMethod.addCode("compositeFuture.onSuccess(r -> promise.complete());\n");
        unregisterMethod.addCode("compositeFuture.onFailure(promise::fail);\n\n");
        unregisterMethod.addCode("return promise.future();");


        // class
        TypeSpec typeBuilder = TypeSpec.classBuilder(fnMessageConsumerRegisterClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get("org.pharosnet.vertx.faas.core.components", "MessageConsumerRegister"))
                .addField(consumersField.build())
                .addMethod(registerMethod.build())
                .addMethod(unregisterMethod.build())
                .build();

        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder)
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
                .indent("\t")
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, fnMessageConsumerRegisterClassName));
    }

}
