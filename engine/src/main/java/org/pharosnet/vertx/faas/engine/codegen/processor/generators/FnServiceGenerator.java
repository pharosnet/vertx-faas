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
        this.generateImplement(fnUnit, filer, typeMirror);
    }

    private void generateInterface(FnUnit fnUnit, Filer filer, TypeMirror typeMirror) throws Exception {
        String pkg = fnUnit.getPackageName();
        String fnClassName = fnUnit.getClassName();
        String serviceClassName = String.format("%sService", fnClassName);
        String serviceImplClassName = String.format("%sServiceImpl", fnClassName);

        // ADDRESS
        String address = String.format("%s-%s", pkg.replace(".", "-"), serviceClassName);
        FieldSpec.Builder staticAddressField = FieldSpec.builder(
                ClassName.get("java.lang", "String"), "SERVICE_ADDRESS",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("$S", address);


        // register
        MethodSpec.Builder registerMethod = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ClassName.get("io.vertx.core", "Vertx"), "vertx")
                .returns(ParameterizedTypeName.get(
                        ClassName.get("io.vertx.core.eventbus", "MessageConsumer"),
                        ClassName.get("io.vertx.core.json", "JsonObject")))
                .addStatement(String.format("return new $T(vertx).setAddress(SERVICE_ADDRESS).register(%s.class, new %s(vertx))", serviceClassName, serviceImplClassName),
                        ClassName.get("io.vertx.serviceproxy", "ServiceBinder"));


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
                .addField(staticAddressField.build())
                .addMethod(registerMethod.build())
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

    private void generateImplement(FnUnit fnUnit, Filer filer, TypeMirror typeMirror) throws Exception {
        String pkg = fnUnit.getPackageName();
        String fnClassName = fnUnit.getClassName();
        String serviceClassName = String.format("%sService", fnClassName);
        String serviceImplClassName = String.format("%sServiceImpl", fnClassName);

        // logger
        FieldSpec.Builder staticLogField = FieldSpec.builder(
                ClassName.get("org.slf4j", "Logger"), "log",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("$T.getLogger($T.class)",
                ClassName.get("org.slf4j", "LoggerFactory"),
                ClassName.get(pkg, serviceImplClassName));

        // construct
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Vertx.class, "vertx")
                .addStatement("this.fn = new $T(vertx)", ClassName.bestGuess(fnUnit.getFn().implementClassFullName()));


        // fn
        FieldSpec.Builder fnField = FieldSpec.builder(
                ClassName.get(pkg, fnClassName), "fn",
                Modifier.PRIVATE, Modifier.FINAL);

        // execute method

        StringBuilder paramsNameBuffer = new StringBuilder();

        MethodSpec.Builder executeMethod = MethodSpec.methodBuilder(fnUnit.getMethodName())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);

        boolean parametersNeedValid = false;
        for (VariableElement parameter : fnUnit.getParameters()) {
            executeMethod.addParameter(
                    ClassName.get(parameter.asType()), parameter.getSimpleName().toString()
            );

            paramsNameBuffer.append(", ").append(parameter.getSimpleName().toString());

            if (parametersNeedValid) {
                continue;
            }

            List<? extends AnnotationMirror> annotationMirrors = parameter.getAnnotationMirrors();
            if (annotationMirrors == null || annotationMirrors.size() == 0) {
                continue;
            }
            for (AnnotationMirror annotation : annotationMirrors) {
                String annotationPkg = annotation.getAnnotationType().toString();
                parametersNeedValid = annotationPkg.startsWith("jakarta.validation");
                if (parametersNeedValid) {
                    break;
                }
            }
        }

        String paramsNames = paramsNameBuffer.toString().substring(2);

        executeMethod.addParameter(
                ParameterizedTypeName.get(
                        ClassName.get("io.vertx.core", "Handler"),
                        ParameterizedTypeName.get(
                                ClassName.get("io.vertx.core", "AsyncResult"),
                                fnUnit.getReturnElementClass()
                        )),
                "handler"
        );

        // valid auth
        if (fnUnit.getFn().authentication()) {
            executeMethod.addCode("// valid auth \n");
            executeMethod.addCode("if (context.getUser() == null) { \n");
            executeMethod.addCode("\thandler.handle($T.fail()); \n", ClassName.get(UnauthorizedException.class));
            executeMethod.addCode("\treturn;\n");
            executeMethod.addCode("} \n");
            executeMethod.addCode("\n");
        }

        // valid parameters
        if (parametersNeedValid) {
            StringBuilder paramsClassBuffer = new StringBuilder();
            for (VariableElement parameter : fnUnit.getParameters()) {
                String parameterClassFullName = ClassName.get(parameter.asType()).toString();
                String parameterClassSimpleName = parameterClassFullName.substring(parameterClassFullName.lastIndexOf(".") + 1);
                paramsClassBuffer.append(", ").append(parameterClassSimpleName).append(".class");
            }
            executeMethod.addCode("// valid parameters \n");
            String paramsValidCode = "try { \n" +
                    "\t$T method = $T.class.getMethod(\"" + fnUnit.getMethodName() + "\" " + paramsClassBuffer.toString() + "); \n" +
                    "\tObject[] parameterValues = new Object[]{" + paramsNames + "}; \n" +
                    "\tboolean valid = $T.validateExecutables(method, fn, parameterValues, handler); \n" +
                    "\tif (!valid) { \n" +
                    "\t\treturn; \n" +
                    "\t} \n" +
                    "} catch (Exception e) { \n" +
                    "\tlog.error(\"参数校验失败！\", e); \n" +
                    "\thandler.handle($T.fail(e)); \n" +
                    "\treturn; \n" +
                    "} \n\n";
            executeMethod.addCode(paramsValidCode,
                    ClassName.get(Method.class),
                    ClassName.bestGuess(fnUnit.getFn().implementClassFullName()),
                    ClassName.get(Validators.class),
                    ClassName.get(SystemException.class)
            );

        }

        // execute
        String executeCode = "this.fn." + fnUnit.getMethodName() + "(" + paramsNames + ") \n" +
                "\t\t.onSuccess(r -> handler.handle($T.succeededFuture(r))) \n" +
                "\t\t.onFailure(e -> { \n" +
                "\t\t\tlog.error(\"{} 执行错误！\", $T.class.toString(), e); \n" +
                "\t\t\thandler.handle($T.fail(e)); \n" +
                "\t\t});\n\n";

        executeMethod.addCode("// execute \n");
        executeMethod.addCode(executeCode,
                ClassName.get(Future.class),
                ClassName.get(pkg, fnClassName),
                ClassName.get(SystemException.class));


        // class
        TypeSpec typeBuilder = TypeSpec.classBuilder(serviceImplClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(pkg, serviceClassName))
                .addField(staticLogField.build())
                .addMethod(constructor.build())
                .addField(fnField.build())
                .addMethod(executeMethod.build())
                .build();

        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder)
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
                .indent("\t")
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, serviceImplClassName));

    }

}
