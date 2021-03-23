package org.pharosnet.vertx.faas.codegen.processor.generators;

import com.squareup.javapoet.*;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.pharosnet.vertx.faas.codegen.annotation.FnRouter;
import org.pharosnet.vertx.faas.codegen.annotation.PathParam;
import org.pharosnet.vertx.faas.codegen.annotation.QueryParam;
import org.pharosnet.vertx.faas.codegen.annotation.RequestBody;
import org.pharosnet.vertx.faas.component.http.HttpMethod;
import org.pharosnet.vertx.faas.context.Context;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Optional;

public class FnRouterGenerator {

    public FnRouterGenerator(Messager messager) {
        this.messager = messager;
    }

    private final Messager messager;

    public void generate(FnUnit fnUnit, Filer filer, TypeMirror typeMirror) throws Exception {
        String pkg = fnUnit.getPackageName();
        String fnClassName = fnUnit.getClassName();
        String serviceClassName = String.format("%sService", fnClassName);
        String fnRouterClassName = String.format("%sRouter", fnClassName);

        // logger
        FieldSpec.Builder staticLogField = FieldSpec.builder(
                ClassName.get("org.slf4j", "Logger"), "log",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("$T.getLogger($T.class)",
                ClassName.get("org.slf4j", "LoggerFactory"),
                ClassName.get(pkg, fnRouterClassName));

        // construct
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Vertx.class, "vertx")
                .addStatement("this.vertx = vertx")
                .addStatement("this.service = $T.proxy(vertx)", ClassName.get(pkg, serviceClassName));


        // vertx
        FieldSpec.Builder vertxField = FieldSpec.builder(
                ClassName.get(Vertx.class), "vertx",
                Modifier.PRIVATE, Modifier.FINAL);
        // vertx
        FieldSpec.Builder serviceField = FieldSpec.builder(
                ClassName.get(pkg, serviceClassName), "service",
                Modifier.PRIVATE, Modifier.FINAL);

        // vertx()
        MethodSpec.Builder vertxMethod = MethodSpec.methodBuilder("vertx")
                .addModifiers(Modifier.PROTECTED)
                .addStatement("return vertx")
                .returns(ClassName.get(Vertx.class));

        // build()
        MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Router.class), "router")
                .returns(ClassName.VOID);
        buildMethod.addCode(String.format("router.%s(\"%s\")\n", fnUnit.getFn().method().toString().toLowerCase(), fnUnit.getFn().path()));
        if (fnUnit.getFn().method().equals(HttpMethod.POST) || fnUnit.getFn().method().equals(HttpMethod.PUT) || fnUnit.getFn().method().equals(HttpMethod.PATCH)) {
            buildMethod.addCode(String.format("\t\t.handler($T.create().setBodyLimit(%d))\n", fnUnit.getFn().bodyLimit()), ClassName.get("io.vertx.ext.web.handler", "BodyHandler"));
        }
        buildMethod.addCode(String.format("\t\t.handler($T.create(%d))\n", fnUnit.getFn().timeout()), ClassName.get("io.vertx.ext.web.handler", "TimeoutHandler"));
        if (fnUnit.getFn().latency()) {
            buildMethod.addCode("\t\t.handler($T.create())\n", ClassName.get("io.vertx.ext.web.handler", "ResponseContentTypeHandler"));
        }
        String produces = Optional.of(fnUnit.getFn().produces()).orElse("").trim().toLowerCase();
        if (produces.length() > 0) {
            buildMethod.addCode(String.format("\t\t.produces(\"%s\")\n", produces));
        }
        String consumes = Optional.of(fnUnit.getFn().consumes()).orElse("").trim().toLowerCase();
        if (consumes.length() > 0) {
            buildMethod.addCode(String.format("\t\t.consumes(\"%s\")\n", consumes));
        }
        buildMethod.addCode("\t\t.handler(this::handle);\n");

        // handle()
        MethodSpec.Builder handleMethod = MethodSpec.methodBuilder("handle")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassName.get(RoutingContext.class), "routingContext")
                .returns(ClassName.VOID);

        handleMethod.addStatement("$T promise = $T.promise()",
                ParameterizedTypeName.get(
                        ClassName.get(Promise.class),
                        fnUnit.getReturnElementClass()
                ),
                ClassName.get(Promise.class));
        handleMethod.addStatement("$T context = $T.fromRoutingContext(routingContext)", ClassName.get(Context.class), ClassName.get(Context.class));

        StringBuilder paramsNameBuffer = new StringBuilder();
        for (VariableElement parameter : fnUnit.getParameters()) {
            String paramName = parameter.getSimpleName().toString();
            paramsNameBuffer.append(", ").append(paramName);

            PathParam pathParam = parameter.getAnnotation(PathParam.class);
            if (pathParam != null) {
                String pathParamKey = pathParam.value();
                handleMethod.addStatement(String.format("$T %s = routingContext.pathParam(\"%s\")", paramName, pathParamKey), parameter.asType());
                continue;
            }
            QueryParam queryParam = parameter.getAnnotation(QueryParam.class);
            if (queryParam != null) {
                String queryParamKey = queryParam.value();
                if (ClassName.get(parameter.asType()).equals(ClassName.get(String.class))) {
                    handleMethod.addStatement(String.format("String %s = null", paramName));
                    handleMethod.addStatement(String.format("List<String> %sValues = routingContext.queryParam(\"%s\")", paramName, queryParamKey));
                    handleMethod.addCode(String.format("if(%sValues != null && !%sValues.isEmpty()) { \n", paramName, paramName));
                    handleMethod.addCode(String.format("\t%s = %sValues.get(0); \n", paramName, paramName));
                    handleMethod.addCode("} \n");
                } else if (ClassName.get(parameter.asType()).equals(ParameterizedTypeName.get(
                        ClassName.get(List.class), ClassName.get(String.class)
                ))) {
                    handleMethod.addStatement(String.format("List<String> %s = routingContext.queryParam(\"%s\")", paramName, queryParamKey));
                } else {
                    throw new RuntimeException(String.format("%s.%s 中的参数 %s 必须是String或List<String>", pkg, fnClassName, paramName));
                }
//                handleMethod.addStatement(String.format("$T %s = $T.routingContext.pathParam(\"%s\")", paramName, pathParamKey), parameter.asType());
                continue;
            }
            RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
            if (requestBody != null) {
                if ("io.vertx.core.json.JsonArray".equals(parameter.asType().toString())) {
                    handleMethod.addStatement(String.format("JsonArray %s = routingContext.getBodyAsJsonArray()", paramName));
                } else {
                    handleMethod.addStatement(String.format("$T %s = new $T(routingContext.getBodyAsJson())", paramName), parameter.asType(), parameter.asType());
                }
            }

        }

        String paramsNames = paramsNameBuffer.toString().substring(2);
        //
        handleMethod.addStatement(String.format("this.service.%s(%s, promise)", fnUnit.getMethodName(), paramsNames));
        handleMethod.addCode("promise.future()\n");
        handleMethod.addCode("\t.onSuccess(r -> {\n");
        handleMethod.addCode("\t\tString result;\n");
        handleMethod.addCode("\t\ttry {\n");
        handleMethod.addCode("\t\t\tresult = r.toJson().encode();\n");
        handleMethod.addCode("\t\t} catch (Throwable throwable) {\n");
        handleMethod.addCode("\t\t\troutingContext.response().setStatusMessage(\"bad code\"); \n");
        handleMethod.addCode("\t\t\troutingContext.response().putHeader(\"x-request-id\", context.getId()); \n");
        handleMethod.addCode("\t\t\troutingContext.fail(555, throwable); \n");
        handleMethod.addCode("\t\t\treturn; \n");
        handleMethod.addCode("\t\t}\n");
        handleMethod.addCode("\t\tlog.debug(\"succeed. {}\", result);\n");
        handleMethod.addCode("\t\troutingContext.response()\n");
        handleMethod.addCode("\t\t\t\t.setStatusCode(200)\n");
        handleMethod.addCode("\t\t\t\t.setChunked(true)\n");
        handleMethod.addCode("\t\t\t\t.putHeader(\"x-request-id\", context.getId())\n");
        handleMethod.addCode("\t\t.end(result, \"UTF-8\"); \n");
        handleMethod.addCode("\t})\n");
        handleMethod.addCode("\t.onFailure(e -> {\n");
        handleMethod.addCode("\t\troutingContext.response().putHeader(\"x-request-id\", context.getId()); \n");
        handleMethod.addCode("\t\troutingContext.fail(505, e); \n");
        handleMethod.addCode("\t}); \n");
        handleMethod.addCode("\t\t \n");

        // class
        TypeSpec typeBuilder = TypeSpec.classBuilder(fnRouterClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(FnRouter.class)
                .addField(staticLogField.build())
                .addMethod(constructor.build())
                .addField(vertxField.build())
                .addField(serviceField.build())
                .addMethod(vertxMethod.build())
                .addMethod(buildMethod.build())
                .addMethod(handleMethod.build())
                .build();

        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder)
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
                .indent("\t")
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, fnRouterClassName));
    }

}
