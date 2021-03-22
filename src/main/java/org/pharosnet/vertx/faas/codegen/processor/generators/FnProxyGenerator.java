package org.pharosnet.vertx.faas.codegen.processor.generators;

import com.squareup.javapoet.*;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

public class FnProxyGenerator {

    public FnProxyGenerator(Messager messager) {
        this.messager = messager;
    }

    private final Messager messager;

    public void generate(FnUnit fnUnit, Filer filer, TypeMirror typeMirror) throws Exception {
        String pkg = fnUnit.getPackageName();
        String fnClassName = fnUnit.getClassName();
        String serviceClassName = String.format("%sService", fnClassName);

        String fnProxyClassName = String.format("%sProxy", fnClassName);

        // construct
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Vertx.class, "vertx")
                .addStatement("this.service = $T.proxy(vertx)", ClassName.get(pkg, serviceClassName));

        // service
        FieldSpec.Builder serviceField = FieldSpec.builder(
                ClassName.get(pkg, serviceClassName), "service",
                Modifier.PRIVATE, Modifier.FINAL);


        // execute
        MethodSpec.Builder executeMethod = MethodSpec.methodBuilder(fnUnit.getMethodName())
                .addModifiers(Modifier.PUBLIC)
                .returns(fnUnit.getReturnClass());

        executeMethod.addStatement("$T promise = $T.promise()",
                ParameterizedTypeName.get(
                        ClassName.get(Promise.class),
                        fnUnit.getReturnElementClass()
                ),
                ClassName.get(Promise.class));


        StringBuilder paramsNameBuffer = new StringBuilder();
        for (VariableElement parameter : fnUnit.getParameters()) {
            String paramName = parameter.getSimpleName().toString();
            paramsNameBuffer.append(", ").append(paramName);
            executeMethod.addParameter(ClassName.get(parameter.asType()), paramName);
        }
        String paramsNames = paramsNameBuffer.toString().substring(2);

        executeMethod.addStatement(String.format("this.service.%s(%s, promise)",
                fnUnit.getMethodName(),
                paramsNames
        ));

        executeMethod.addStatement("return promise.future()");

        // class
        TypeSpec typeBuilder = TypeSpec.classBuilder(fnProxyClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(pkg, fnClassName))
                .addMethod(constructor.build())
                .addField(serviceField.build())
                .addMethod(executeMethod.build())
                .build();

        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder)
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
                .indent("\t")
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, fnProxyClassName));
    }
}
