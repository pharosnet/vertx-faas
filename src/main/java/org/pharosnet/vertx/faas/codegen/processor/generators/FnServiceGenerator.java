package org.pharosnet.vertx.faas.codegen.processor.generators;

import com.squareup.javapoet.*;
import io.vertx.core.Vertx;
import org.pharosnet.vertx.faas.exception.SystemException;
import org.pharosnet.vertx.faas.validator.Validators;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

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

        // ADDRESS
        String address = String.format("%s-%s", fnUnit.getFn().module(), fnUnit.getFn().id());
        FieldSpec.Builder staticAddressField = FieldSpec.builder(
                ClassName.get("java.lang", "String"), "SERVICE_ADDRESS",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("$S", address);

        ;
        // register
        MethodSpec.Builder registerMethod = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ClassName.get("io.vertx.core", "Vertx"), "vertx")
                .returns(ParameterizedTypeName.get(
                        ClassName.get("io.vertx.core.eventbus", "MessageConsumer"),
                        ClassName.get("io.vertx.core.json", "JsonObject")))
                .addStatement(String.format("return new $T(vertx).setAddress(SERVICE_ADDRESS).register(%s.class, new $T(vertx));", serviceClassName),
                        ClassName.get("io.vertx.serviceproxy", "ServiceBinder"),
                        ClassName.get(fnUnit.getFn().implement()));


        // proxy
        MethodSpec.Builder proxyMethod = MethodSpec.methodBuilder("proxy")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ClassName.get("io.vertx.core", "Vertx"), "vertx")
                .returns(ClassName.get(pkg, serviceClassName))
                .addStatement("return new $T(vertx, SERVICE_ADDRESS);",
                        ClassName.get(pkg, "UserGetFnServiceVertxEBProxy"),
                        ClassName.get(fnUnit.getFn().implement()));

        // execute
        MethodSpec.Builder executeMethod = MethodSpec.methodBuilder("execute")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);

        for (Parameter parameter : fnUnit.getParameters()) {
            executeMethod.addParameter(
                    ClassName.get(parameter.getType()), parameter.getName()
            );
        }
        executeMethod.addParameter(
                ParameterizedTypeName.get(
                        ClassName.get("io.vertx.core", "Handler"),
                        ParameterizedTypeName.get(
                                ClassName.get("io.vertx.core", "AsyncResult"),
                                ClassName.get(fnUnit.getReturnElementClass())
                        )),
                "handler"
        );

        // interface
        TypeSpec typeBuilder = TypeSpec.interfaceBuilder(serviceClassName)
                .addModifiers(Modifier.PUBLIC)
                .addField(staticAddressField.build())
                .addMethod(registerMethod.build())
                .addMethod(proxyMethod.build())
                .addMethod(executeMethod.build())
                .build();

        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder)
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
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
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("$T.getLogger($S.class)",
                ClassName.get("org.slf4j", "LoggerFactory"),
                serviceImplClassName);

        // construct
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Vertx.class, "vertx")
                .addStatement("this.fn = new $T(vertx);", ClassName.get(fnUnit.getFn().implement()));


        // fn
        FieldSpec.Builder fnField = FieldSpec.builder(
                ClassName.get(pkg, fnClassName), "fn",
                Modifier.PRIVATE, Modifier.FINAL);

        // execute method

        StringBuilder paramsNameBuffer = new StringBuilder();

        MethodSpec.Builder executeMethod = MethodSpec.methodBuilder("execute")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);

        boolean parametersNeedValid = false;
        for (Parameter parameter : fnUnit.getParameters()) {
            executeMethod.addParameter(
                    ClassName.get(parameter.getType()), parameter.getName()
            );

            paramsNameBuffer.append(", ").append(parameter.getName());

            if (parametersNeedValid) {
                continue;
            }
            Annotation[] annotations = parameter.getAnnotations();
            if (annotations == null || annotations.length == 0) {
                continue;
            }
            for (Annotation annotation : annotations) {
                String annotationPkg = annotation.annotationType().getPackageName();
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
                                ClassName.get(fnUnit.getReturnElementClass())
                        )),
                "handler"
        );

        // valid auth
        if (fnUnit.getFn().authentication()) {
            executeMethod.addStatement("// valid auth");
            executeMethod.addStatement("if (context.getUser() == null) {");
            executeMethod.addStatement("handler.handle(UnauthorizedException.fail());");
            executeMethod.addStatement("return;");
            executeMethod.addStatement("}");
            executeMethod.addStatement("");
        }

        // valid parameters
        if (parametersNeedValid) {
            StringBuilder paramsClassBuffer = new StringBuilder();
            for (Parameter parameter : fnUnit.getParameters()) {
                paramsClassBuffer.append(", ").append(parameter.getType().getSimpleName()).append(".class");
            }
            executeMethod.addStatement("// valid parameters");
            executeMethod.addStatement("try {");
            //  Method method = UserGetFnImpl.class.getMethod("execute", Context.class, String.class);
            executeMethod.addStatement("$T method = $S.class.getMethod(\"execute\" " + paramsClassBuffer.toString() + ");",
                    ClassName.get(Method.class),
                    ClassName.get(fnUnit.getFn().implement()));
            executeMethod.addStatement("Object[] parameterValues = new Object[]{" + paramsNames + "};");
            executeMethod.addStatement("boolean valid = $T.validateExecutables(method, fn, parameterValues, handler);",
                    ClassName.get(Validators.class));
            executeMethod.addStatement(" if (!valid) {");
            executeMethod.addStatement("return;");
            executeMethod.addStatement("}");
            executeMethod.addStatement("catch (Exception e) {");
            executeMethod.addStatement("log.error(\"参数校验失败！\", e);");
            executeMethod.addStatement(" handler.handle(SystemException.fail(e));");
            executeMethod.addStatement("return;");
            executeMethod.addStatement("}");
            executeMethod.addStatement("");

        }

        // execute
        executeMethod.addStatement("this.fn.execute(" + paramsNames + ")");
        executeMethod.addStatement(".onSuccess(r -> handler.handle(Future.succeededFuture(r)))");
        executeMethod.addStatement(".onFailure(e -> {");
        executeMethod.addStatement(" log.error(\"{} 执行错误！\", $T.class.toString(), e);", ClassName.get(pkg, fnClassName));
        executeMethod.addStatement(" handler.handle($T.fail(e));", ClassName.get(SystemException.class));
        executeMethod.addStatement("});");
        executeMethod.addStatement("");

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
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, serviceImplClassName));

    }

}
