package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.*;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DALMethodGenerator {

    public void generate(TypeSpec.Builder typeBuilder, DALMethodModel methodModel) {
        typeBuilder.addField(this.generateSQL(methodModel));
        typeBuilder.addMethod(this.generateMethod(methodModel));
    }

    // return has
    public FieldSpec generateSQL(DALMethodModel methodModel) {
        String sql = methodModel.getQuery().value();
        FieldSpec.Builder staticSqlField = FieldSpec.builder(
                ClassName.get(String.class), methodModel.getSqlFieldName(),
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("$S", sql);
        return staticSqlField.build();
    }

    public MethodSpec generateMethod(DALMethodModel model) {
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder(model.getName());
        methodBuild.addModifiers(Modifier.PUBLIC);
        methodBuild.returns(model.getReturnClassName());
        for (DALMethodParamModel param : model.getParamModels()) {
            methodBuild.addParameter(param.getParamClassName(), param.getParamName());
        }
        methodBuild.addCode("$T promise = $T.promise();\n",
                ParameterizedTypeName.get(
                        ClassName.get(Promise.class),
                        model.getReturnTopParameterizedTypeName()
                ),
                ClassName.get(Promise.class)
        );


        if (model.isHasPlaceholder()) {
            StringBuilder code = new StringBuilder("String sql = " + model.getSqlFieldName());
            for (String k : model.getPlaceholders().keySet()) {
                code.append(".replace(\"#").append(k).append("#\", ").append(model.getPlaceholders().get(k)).append(" );\n");
            }
            methodBuild.addCode(code.toString());
        }

        methodBuild.addCode("$T args = new $T();\n",
                ClassName.get(JsonArray.class),
                ClassName.get(JsonArray.class)
        );
        for (String argTypeName : model.getParamArgNames()) {
            methodBuild.addCode(String.format("args.add(%s);\n", argTypeName));
        }


        methodBuild.addCode("$T arg = new $T();\n",
                ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"),
                ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg")
        );
        if (model.isHasPlaceholder()) {
            methodBuild.addCode("arg.setQuery(sql);\n");
        } else {
            methodBuild.addCode(String.format("arg.setQuery(%s);\n", model.getSqlFieldName()));
        }
        methodBuild.addCode("arg.setArgs(args);\n");
        methodBuild.addCode("arg.setBatch(false);\n\n");
        if (model.getQuery().slaverMode()) {
            methodBuild.addCode("arg.setSlaverMode(true);\n");
        } else {
            methodBuild.addCode("arg.setSlaverMode(false);\n");
        }
        methodBuild.addCode(String.format("arg.setNeedLastInsertedId(%b);\n", model.getQuery().needLastInsertedId()));
        methodBuild.addCode("\n");


        methodBuild.addCode("this.service().query(context, arg, r -> {\n");
        methodBuild.addCode("\tif (r.failed()) {\n");
        methodBuild.addCode(String.format("\t\tlog.error(\"%s failed\", r.cause());\n", model.getName()));
        methodBuild.addCode("\t\tpromise.fail(r.cause());\n");
        methodBuild.addCode("\t\treturn;\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\t$T queryResult = r.result();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryResult"));
        methodBuild.addCode("\tif (log.isDebugEnabled()) {\n");
        methodBuild.addCode(String.format("\t\tlog.debug(\"%s succeed, latency = {}\", queryResult.getLatency());\n", model.getName()));
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\n");
        if (model.isSingleReturn()) {
            methodBuild.addCode("\t$T value = null;\n", model.getReturnElementClassName());
            methodBuild.addCode("\tif (queryResult.getRows() != null && queryResult.getRows().size() > 0) {\n");
            methodBuild.addCode(String.format("\t\tvalue = new %sMapper().map(queryResult.getRows().get(0));\n", ((ClassName) model.getReturnElementClassName()).simpleName()));
            methodBuild.addCode("\t}\n");
            if (model.isReturnWithOptional()) {
                methodBuild.addCode("\tpromise.complete($T.ofNullable(value));\n", ClassName.get(Optional.class));
            } else {
                methodBuild.addCode("\tpromise.complete(value);\n");
            }
            methodBuild.addCode("});\n");
            methodBuild.addCode("return promise.future();\n");
        } else {
            methodBuild.addCode("\t$T values = null;\n",
                    ParameterizedTypeName.get(
                            ClassName.get(List.class),
                            model.getReturnElementClassName()
                    )
            );
            methodBuild.addCode("\tif (queryResult.getRows() != null && queryResult.getRows().size() > 0) {\n");
            methodBuild.addCode("\t\tvalues = new $T<>(queryResult.getRows().size());\n",
                    ClassName.get(ArrayList.class)
            );
            methodBuild.addCode(String.format("\t\t%sMapper mapper = new %sMapper();\n",
                    ((ClassName) model.getReturnElementClassName()).simpleName(),
                    ((ClassName) model.getReturnElementClassName()).simpleName()
                    )
            );
            methodBuild.addCode("\t\tfor ($T _row : queryResult.getRows()) {\n", ClassName.get(JsonObject.class));
            methodBuild.addCode("\t\t\tvalues.add(mapper.map(_row));\n");
            methodBuild.addCode("\t\t}\n");
            methodBuild.addCode("\t}\n");
            if (model.isReturnWithOptional()) {
                if (model.isReturnStream()) {
                    methodBuild.addCode("\tpromise.complete($T.ofNullable(values.stream()));\n", ClassName.get(Optional.class));
                } else {
                    methodBuild.addCode("\tpromise.complete($T.ofNullable(values));\n", ClassName.get(Optional.class));
                }
            } else {
                if (model.isReturnStream()) {
                    methodBuild.addCode("\tpromise.complete(values.stream());\n");
                } else {
                    methodBuild.addCode("\tpromise.complete(values);\n");
                }
            }
            methodBuild.addCode("});\n");
            methodBuild.addCode("return promise.future();\n");

        }

        return methodBuild.build();
    }

}
