package org.pharosnet.vertx.faas.codegen.processor.generators;

import com.squareup.javapoet.*;
import io.vertx.codegen.annotations.ModuleGen;
import io.vertx.codegen.format.CamelCase;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.component.MessageConsumerRegister;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

public class MessageConsumerRegisterGenerator {

    public MessageConsumerRegisterGenerator(Messager messager, Elements elementUtils, Filer filer) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.filer = filer;
    }

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;

    public void generate(String pkg, ModuleGen moduleGen, List<Element> fnElements) throws Exception {
        String fnMessageConsumerRegisterClassName = String.format("%sMessageConsumerRegister", CamelCase.INSTANCE.format(List.of(moduleGen.name())));


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
                                )), ClassName.get(ArrayList.class))
                .returns(ParameterizedTypeName.get(
                        ClassName.get(List.class),
                        ParameterizedTypeName.get(
                                ClassName.get(MessageConsumer.class),
                                ClassName.get(JsonObject.class)
                        )));

        for (Element element : fnElements) {
            String fnServiceClassName = String.format("%sService", element.getSimpleName());
            registerMethod.addStatement("consumers.add($T.register(vertx))", ClassName.get(pkg, fnServiceClassName));
        }
        registerMethod.addStatement("return consumers");

        // class
        TypeSpec typeBuilder = TypeSpec.classBuilder(fnMessageConsumerRegisterClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(MessageConsumerRegister.class))
                .addMethod(registerMethod.build())
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
