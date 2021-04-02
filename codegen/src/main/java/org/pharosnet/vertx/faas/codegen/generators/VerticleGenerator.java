package org.pharosnet.vertx.faas.codegen.generators;

import com.squareup.javapoet.*;
import io.vertx.codegen.format.CamelCase;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.pharosnet.vertx.faas.codegen.annotation.FnModule;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

public class VerticleGenerator {

    public VerticleGenerator(Messager messager, Elements elementUtils, Filer filer) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.filer = filer;
    }

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;

    public void generate(String pkg, FnModule fnModule) throws Exception {
        List<String> nameItems = new ArrayList<>(List.of(fnModule.name().split("-")));
        String fnMessageConsumerRegisterClassName = String.format("%sMessageConsumerRegister", CamelCase.INSTANCE.format(nameItems));

        nameItems.add("verticle");
        String fnVerticleClassName = CamelCase.INSTANCE.format(nameItems);


        // construct
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        // register
        FieldSpec.Builder registerField = FieldSpec.builder(
                ClassName.get("org.pharosnet.vertx.faas.core.components", "MessageConsumerRegister"), "register",
                Modifier.PRIVATE);

        // register()
        MethodSpec.Builder registerMethod = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PRIVATE)
                .addCode("this.register = new $T();\n", ClassName.get(pkg, fnMessageConsumerRegisterClassName))
                .addCode("this.register.register(this.vertx);")
                .returns(ClassName.VOID);

        // unregister()
        MethodSpec.Builder unregisterMethod = MethodSpec.methodBuilder("unregister")
                .addModifiers(Modifier.PRIVATE)
                .addCode("if (this.register == null) {\n")
                .addCode("\treturn $T.succeededFuture();\n", ClassName.get(Future.class))
                .addCode("}\n")
                .addCode("return this.register.unregister();")
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
