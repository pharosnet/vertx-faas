package org.pharosnet.vertx.faas.engine.codegen.processor.generators;

import com.squareup.javapoet.*;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.pharosnet.vertx.faas.core.exceptions.SystemException;
import org.pharosnet.vertx.faas.core.exceptions.UnauthorizedException;
import org.pharosnet.vertx.faas.engine.validator.Validators;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.reflect.Method;
import java.util.List;

public class FnServiceGenerator {

    public FnServiceGenerator(Messager messager) {
        this.messager = messager;
    }

    private final Messager messager;

    public void generate(FnUnit fnUnit, Filer filer, TypeMirror typeMirror) throws Exception {
        this.generateInterface(fnUnit, filer, typeMirror);
    }

    private void generateInterface(FnUnit fnUnit, Filer filer, TypeMirror typeMirror) throws Exception {
        String pkg = fnUnit.getPackageName();
        String fnClassName = fnUnit.getClassName();
        String serviceClassName = String.format("%sService", fnClassName);

        // NAME
        String name = String.format("%s-%s", pkg.replace(".", "-"), serviceClassName);
        FieldSpec.Builder staticNameField = FieldSpec.builder(
                ClassName.get("java.lang", "String"), "SERVICE_NAME",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("$S", name);

        // ADDRESS
        String address = String.format("%s-%s", pkg.replace(".", "-"), serviceClassName);
        FieldSpec.Builder staticAddressField = FieldSpec.builder(
                ClassName.get("java.lang", "String"), "SERVICE_ADDRESS",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("$S", address);


        // proxy
        MethodSpec.Builder proxyMethod = MethodSpec.methodBuilder("proxy")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ClassName.get("io.vertx.core", "Vertx"), "vertx")
                .returns(ClassName.get(pkg, serviceClassName))
                .addStatement("return new $T(vertx, SERVICE_ADDRESS)",
                        ClassName.get(pkg, String.format("%sServiceVertxEBProxy", fnUnit.getClassName())));

        // execute
        MethodSpec.Builder executeMethod = MethodSpec.methodBuilder("execute")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.VOID);

        for (VariableElement parameter : fnUnit.getParameters()) {
            executeMethod.addParameter(
                    ClassName.get(parameter.asType()), parameter.getSimpleName().toString()
            );
        }
        executeMethod.addParameter(
                ParameterizedTypeName.get(
                        ClassName.get("io.vertx.core", "Handler"),
                        ParameterizedTypeName.get(
                                ClassName.get("io.vertx.core", "AsyncResult"),
                                fnUnit.getReturnElementClass()
                        )),
                "handler"
        );

        // interface
        TypeSpec typeBuilder = TypeSpec.interfaceBuilder(serviceClassName)
                .addAnnotation(VertxGen.class)
                .addAnnotation(ProxyGen.class)
                .addModifiers(Modifier.PUBLIC)
                .addField(staticNameField.build())
                .addField(staticAddressField.build())
                .addMethod(proxyMethod.build())
                .addMethod(executeMethod.build())
                .build();

        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder)
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
                .indent("\t")
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, serviceClassName));
    }

}
