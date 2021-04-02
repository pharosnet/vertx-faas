package org.pharosnet.vertx.faas.codegen.generators;

import com.squareup.javapoet.*;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.pharosnet.vertx.faas.codegen.annotation.*;
import org.pharosnet.vertx.faas.codegen.http.HttpMethod;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Optional;

public class FnRouterGenerator {

    public FnRouterGenerator(Elements elementUtils, Messager messager) {
        this.elementUtils = elementUtils;
        this.messager = messager;
    }

    private final Elements elementUtils;
    private final Messager messager;

    public void generate(FnImpl fnImpl, Filer filer, TypeMirror typeMirror) throws Exception {
        String pkg = fnImpl.getPkg();
        String fnClassName = fnImpl.getFnUnit().getClassName();
        String servicePkg = fnImpl.getFnUnit().getPackageName();
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
                .addStatement("this.service = $T.proxy(vertx)", ClassName.get(servicePkg, serviceClassName));


        // vertx
        FieldSpec.Builder vertxField = FieldSpec.builder(
                ClassName.get(Vertx.class), "vertx",
                Modifier.PRIVATE, Modifier.FINAL);
        // service
        FieldSpec.Builder serviceField = FieldSpec.builder(
                ClassName.get(servicePkg, serviceClassName), "service",
                Modifier.PRIVATE, Modifier.FINAL);

        // vertx()
        MethodSpec.Builder vertxMethod = MethodSpec.methodBuilder("vertx")
                .addModifiers(Modifier.PROTECTED)
                .addStatement("return vertx")
                .returns(ClassName.get(Vertx.class));

        // build()
        MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("io.vertx.ext.web", "Router"), "router")
                .returns(ClassName.VOID);
        String path = Optional.of(fnImpl.getFnUnit().getFn().path()).orElse("").trim();
        if (path.length() == 0) {
            path = String.format("/%s/%s", fnImpl.getFnModule().name(), fnClassName);
        }
        buildMethod.addCode(String.format("router.%s(\"%s\")\n", fnImpl.getFnUnit().getFn().method().toString().toLowerCase(), path));

        String produces = Optional.of(fnImpl.getFnUnit().getFn().produces()).orElse("").trim().toLowerCase();
        if (produces.length() > 0) {
            buildMethod.addCode(String.format("\t\t.produces(\"%s\")\n", produces));
            buildMethod.addCode(String.format("\t\t.handler($T.create(\"%s\"))\n", produces), ClassName.get("org.pharosnet.vertx.faas.engine.http.handler", "ResponseContentTypeHeader"));
        }
        String consumes = Optional.of(fnImpl.getFnUnit().getFn().consumes()).orElse("").trim().toLowerCase();
        if (consumes.length() > 0) {
            buildMethod.addCode(String.format("\t\t.consumes(\"%s\")\n", consumes));
        }
        if (fnImpl.getFnUnit().getFn().method().equals(HttpMethod.POST) || fnImpl.getFnUnit().getFn().method().equals(HttpMethod.PUT) || fnImpl.getFnUnit().getFn().method().equals(HttpMethod.PATCH)) {
            buildMethod.addCode(String.format("\t\t.handler($T.create().setBodyLimit(%d))\n", fnImpl.getFnUnit().getFn().bodyLimit()), ClassName.get("io.vertx.ext.web.handler", "BodyHandler"));
        }
        buildMethod.addCode(String.format("\t\t.handler($T.create(%d))\n", fnImpl.getFnUnit().getFn().timeout()), ClassName.get("io.vertx.ext.web.handler", "TimeoutHandler"));
        if (fnImpl.getFnUnit().getFn().latency()) {
            buildMethod.addCode("\t\t.handler($T.create())\n", ClassName.get("io.vertx.ext.web.handler", "ResponseTimeHandler"));
        }

        if (fnImpl.getFnInterceptor() != null && fnImpl.getFnInterceptor().before().length > 0) {
            for (String interceptorClassName : fnImpl.getFnInterceptor().before()) {
                ClassName interceptor = ClassName.bestGuess(interceptorClassName);
                buildMethod.addCode("\t\t.handler(new $T(vertx))\n", interceptor);
            }
        }

        buildMethod.addCode("\t\t.handler(this::handle)\n");

        if (fnImpl.getFnInterceptor() != null && fnImpl.getFnInterceptor().after().length > 0) {
            for (String interceptorClassName : fnImpl.getFnInterceptor().after()) {
                ClassName interceptor = ClassName.bestGuess(interceptorClassName);
                buildMethod.addCode("\t\t.handler(new $T(vertx))\n", interceptor);
            }
        }

        buildMethod.addCode("\t\t.handler(ctx -> ctx.response().end());\n");
        // handle()
        MethodSpec.Builder handleMethod = MethodSpec.methodBuilder("handle")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassName.get("io.vertx.ext.web", "RoutingContext"), "routingContext")
                .returns(ClassName.VOID);

        handleMethod.addStatement("$T promise = $T.promise()",
                ParameterizedTypeName.get(
                        ClassName.get(Promise.class),
                        fnImpl.getFnUnit().getReturnElementClass()
                ),
                ClassName.get(Promise.class));
        handleMethod.addStatement("$T context = $T.fromRoutingContext(routingContext)",
                ClassName.get("org.pharosnet.vertx.faas.http.context", "FnContext"),
                ClassName.get("org.pharosnet.vertx.faas.http.context", "FnContext"));

        StringBuilder paramsNameBuffer = new StringBuilder();
        for (VariableElement parameter : fnImpl.getFnUnit().getParameters()) {
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
                    handleMethod.addStatement(String.format("$T %sValues = routingContext.queryParam(\"%s\")", paramName, queryParamKey),
                            ParameterizedTypeName.get(
                                    ClassName.get(List.class),
                                    ClassName.get(String.class)
                            )
                    );
                    handleMethod.addCode(String.format("if(%sValues != null && !%sValues.isEmpty()) { \n", paramName, paramName));
                    handleMethod.addCode(String.format("\t%s = %sValues.get(0); \n", paramName, paramName));
                    handleMethod.addCode("} \n");
                } else if (ClassName.get(parameter.asType()).equals(ParameterizedTypeName.get(
                        ClassName.get(List.class), ClassName.get(String.class)
                ))) {
                    handleMethod.addStatement(String.format("$T %s = routingContext.queryParam(\"%s\")", paramName, queryParamKey),
                            ParameterizedTypeName.get(
                                    ClassName.get(List.class),
                                    ClassName.get(String.class)
                            )
                    );
                } else {
                    throw new RuntimeException(String.format("%s.%s 中的参数 %s 必须是String或List<String>", pkg, fnClassName, paramName));
                }
                continue;
            }

            HeaderParam headerParam = parameter.getAnnotation(HeaderParam.class);
            if (headerParam != null) {
                handleMethod.addStatement(String.format("String %s = routingContext.request().getHeader(\"%s\")",
                        paramName, paramName
                ));
                continue;
            }

            CookieParam cookieParam = parameter.getAnnotation(CookieParam.class);
            if (cookieParam != null) {
                handleMethod.addStatement(String.format("String %s = null", paramName));
                handleMethod.addCode(String.format("$T %sCookie = routingContext.getCookie(\"%s\");\n",
                        paramName, paramName
                        ),
                        ClassName.get("io.vertx.core.http", "Cookie")
                );
                handleMethod.addCode(String.format("if (%sCookie != null) {\n", paramName));
                handleMethod.addCode(String.format("\t%s = %sCookie.getValue();\n", paramName, paramName));
                handleMethod.addCode("}\n");
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
        handleMethod.addStatement(String.format("this.service.%s(%s, promise)", fnImpl.getFnUnit().getMethodName(), paramsNames));
        handleMethod.addCode("promise.future()\n");
        handleMethod.addCode("\t.onSuccess(r -> {\n");
        handleMethod.addCode("\t\tString result;\n");
        handleMethod.addCode("\t\tif (r == null) {\n");
        handleMethod.addCode("\t\t\troutingContext.fail(404, new Exception(\"返回空对象。\"));\n");
        handleMethod.addCode("\t\t\treturn;\n");
        handleMethod.addCode("\t\t}\n");

        handleMethod.addCode("\t\ttry {\n");
        handleMethod.addCode("\t\t\tresult = r.toJson().encode();\n");
        handleMethod.addCode("\t\t} catch (Throwable throwable) {\n");
        handleMethod.addCode("\t\t\troutingContext.response().setStatusMessage(\"bad code\"); \n");
        handleMethod.addCode("\t\t\troutingContext.response().putHeader(\"x-request-id\", context.getId()); \n");
        handleMethod.addCode("\t\t\troutingContext.fail(throwable); \n");
        handleMethod.addCode("\t\t\treturn; \n");
        handleMethod.addCode("\t\t}\n");
        handleMethod.addCode("\t\troutingContext.response()\n");
        handleMethod.addCode(String.format("\t\t\t\t.setStatusCode(%d)\n", fnImpl.getFnUnit().getFn().succeedStatus()));
        handleMethod.addCode("\t\t\t\t.setChunked(true)\n");
        handleMethod.addCode("\t\t\t\t.putHeader(\"x-request-id\", context.getId())");
        if (fnImpl.getFnUnit().getReturnElementClass().equals(TypeName.VOID)) {
            handleMethod.addCode("; \n");
        } else {
            handleMethod.addCode("\n\t\t\t\t.write(result, \"UTF-8\");\n");
        }
        handleMethod.addCode("\t\troutingContext.next(); \n");
        handleMethod.addCode("\t})\n");
        handleMethod.addCode("\t.onFailure(e -> {\n");
        handleMethod.addCode("\t\troutingContext.response().putHeader(\"x-request-id\", context.getId()); \n");
        handleMethod.addCode("\t\troutingContext.fail(505, e); \n");
        handleMethod.addCode("\t}); \n");
        handleMethod.addCode("\t\t \n");

        // class
        TypeSpec typeBuilder = TypeSpec.classBuilder(fnRouterClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.pharosnet.vertx.faas.core.annotations", "FnRouter"))
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
