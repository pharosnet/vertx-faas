package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.*;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.database.codegen.DatabaseType;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class DALGetGenerator {

    public DALGetGenerator(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    private final DatabaseType databaseType;

    public void generate(DALModel dalModel, TypeSpec.Builder typeBuilder) {

        String sql = this.generateSQL(dalModel.getTableModel());

        // sql
        FieldSpec.Builder staticSqlField = FieldSpec.builder(
                ClassName.get(String.class), "_getSQL",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("$S", sql);
        typeBuilder.addField(staticSqlField.build());

        typeBuilder.addMethod(this.generateOne(dalModel).build());
        typeBuilder.addMethod(this.generateBatch(dalModel).build());

    }

    public String generateSQL(TableModel tableModel) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT").append(" ");
        StringBuilder columns = new StringBuilder();
        String idColumnName = "";
        for (ColumnModel columnModel : tableModel.getColumnModels()) {
            columns.append(", ").append(columnModel.getColumn().name());
            if (columnModel.getKind().equals(ColumnKind.ID)) {
                idColumnName = columnModel.getColumn().name();
            }
        }
        builder.append(columns.toString().substring(2)).append(" ");
        builder.append("FROM").append(" ");
        if (tableModel.getTable().schema().length() > 0) {
            builder.append(tableModel.getTable().schema().toUpperCase()).append(".");
        }
        builder.append(tableModel.getTable().name()).append(" ");
        builder.append("WHERE").append(" ").append(idColumnName).append(" = ");
        if (databaseType.equals(DatabaseType.MYSQL)) {
            builder.append("?");
        } else {
            builder.append("$1");
        }
        return builder.toString().toUpperCase();
    }

    public MethodSpec.Builder generateOne(DALModel dalModel) {
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("org.pharosnet.vertx.faas.database.api", "SqlContext"), "context")
                .addParameter(dalModel.getIdClassName(), "id")
                .returns(
                        ParameterizedTypeName.get(
                                ClassName.get(Future.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(Optional.class),
                                        dalModel.getTableClassName()
                                )
                        )
                )
                .addStatement("$T promise = $T.promise()",
                        ParameterizedTypeName.get(
                                ClassName.get(Promise.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(Optional.class),
                                        dalModel.getTableClassName()
                                )
                        ),
                        ClassName.get(Promise.class)
                );


        methodBuild.addCode("$T args = new $T();\n",
                ClassName.get(JsonArray.class),
                ClassName.get(JsonArray.class)
        );

        methodBuild.addCode("args.add(id);\n");


        methodBuild
                .addCode("$T arg = new $T();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"), ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"))
                .addCode("arg.setQuery(_getSQL);\n")
                .addCode("arg.setArgs(args);\n")
                .addCode("arg.setBatch(false);\n")
                .addCode("arg.setSlaverMode(false);\n")
                .addCode("arg.setNeedLastInsertedId(false);\n");


        methodBuild.addCode("this.service().query(context, arg, r -> {\n");
        methodBuild.addCode("\tif (r.failed()) {\n");
        methodBuild.addCode("\t\tlog.error(\"get failed\", r.cause());\n");
        methodBuild.addCode("\t\tpromise.fail(r.cause());\n");
        methodBuild.addCode("\t\treturn;\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\t$T queryResult = r.result();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryResult"));
        methodBuild.addCode("\tif (log.isDebugEnabled()) {\n");
        methodBuild.addCode("\t\tlog.debug(\"get succeed, latency = {}\", queryResult.getLatency());\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\n");

        methodBuild.addCode("\t$T value = null;\n", dalModel.getTableClassName());
        methodBuild.addCode("\tif (queryResult.getRows() != null && queryResult.getRows().size() > 0) {\n");
        methodBuild.addCode("\t\tvalue = new $T().map(queryResult.getRows().get(0));\n", dalModel.getTableMapperClassName());
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\tpromise.complete($T.ofNullable(value));\n", ClassName.get(Optional.class));
        methodBuild.addCode("});\n");
        methodBuild.addCode("return promise.future();\n");


        return methodBuild;
    }

    public MethodSpec.Builder generateBatch(DALModel dalModel) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT").append(" ");
        StringBuilder columns = new StringBuilder();
        String idColumnName = "";
        for (ColumnModel columnModel : dalModel.getTableModel().getColumnModels()) {
            columns.append(", ").append(columnModel.getColumn().name());
            if (columnModel.getKind().equals(ColumnKind.ID)) {
                idColumnName = columnModel.getColumn().name();
            }
        }
        builder.append(columns.toString().substring(2)).append(" ");
        builder.append("FROM").append(" ");
        if (dalModel.getTableModel().getTable().schema().length() > 0) {
            builder.append(dalModel.getTableModel().getTable().schema().toUpperCase()).append(".");
        }
        builder.append(dalModel.getTableModel().getTable().name()).append(" ");
        builder.append("WHERE").append(" ").append(idColumnName).append(" IN (#ids#)");
        String getBatchSQL = builder.toString();
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("org.pharosnet.vertx.faas.database.api", "SqlContext"), "context")
                .addParameter(ParameterizedTypeName.get(
                        ClassName.get(Stream.class),
                        dalModel.getIdClassName()
                ), "ids")
                .returns(
                        ParameterizedTypeName.get(
                                ClassName.get(Future.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(Optional.class),
                                        ParameterizedTypeName.get(
                                                ClassName.get(Stream.class),
                                                dalModel.getTableClassName()
                                        )
                                )
                        )
                );

        methodBuild.addCode("if (ids == null || ids.count() == 0) {\n");
        methodBuild.addCode("\treturn Future.failedFuture(\"ids is empty\");\n");
        methodBuild.addCode("}\n");

        methodBuild.addStatement("$T promise = $T.promise()",
                ParameterizedTypeName.get(
                        ClassName.get(Promise.class),
                        ParameterizedTypeName.get(
                                ClassName.get(Optional.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(Stream.class),
                                        dalModel.getTableClassName()
                                )
                        )
                ),
                ClassName.get(Promise.class)
        );

        methodBuild.addCode("String _getBatchSQL = $S;\n", getBatchSQL);

        if (dalModel.isIdTypeString()) {
            methodBuild.addCode("_getBatchSQL = _getBatchSQL.replace(\"#ids#\"," +
                    " ids.map(v -> String.format(\"'%s'\", v)).reduce((v1, v2) -> String.format(\"%s, %s\", v1, v2)).orElse(\"NULL\"));\n");
        } else {
            methodBuild.addCode("_getBatchSQL = _getBatchSQL.replace(\"#ids#\"," +
                    " ids.map(v -> String.format(\"%s\", v)).reduce((v1, v2) -> String.format(\"%s, %s\", v1, v2)).orElse(\"NULL\"));\n");
        }


        methodBuild.addCode("$T arg = new $T();\n",
                ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"),
                ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg")
        );
        methodBuild.addCode("arg.setQuery(_getBatchSQL);\n");
        methodBuild.addCode("arg.setBatch(false);\n\n");
        methodBuild.addCode("arg.setSlaverMode(false);\n");
        methodBuild.addCode("arg.setNeedLastInsertedId(false);\n");
        methodBuild.addCode("\n");

        methodBuild.addCode("this.service().query(context, arg, r -> {\n");
        methodBuild.addCode("\tif (r.failed()) {\n");
        methodBuild.addCode("\t\tlog.error(\"get batch failed\", r.cause());\n");
        methodBuild.addCode("\t\tpromise.fail(r.cause());\n");
        methodBuild.addCode("\t\treturn;\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\t$T queryResult = r.result();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryResult"));
        methodBuild.addCode("\tif (log.isDebugEnabled()) {\n");
        methodBuild.addCode("\t\tlog.debug(\"get batch succeed, latency = {}\", queryResult.getLatency());\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\n");

        methodBuild.addCode("\t$T values = null;\n",
                ParameterizedTypeName.get(
                        ClassName.get(List.class),
                        dalModel.getTableClassName()
                )
        );
        methodBuild.addCode("\tif (queryResult.getRows() != null && queryResult.getRows().size() > 0) {\n");
        methodBuild.addCode("\t\tvalues = new $T<>(queryResult.getRows().size());\n",
                ClassName.get(ArrayList.class)
        );
        methodBuild.addCode("\t\t$T mapper = new $T();\n",
                dalModel.getTableMapperClassName(),
                dalModel.getTableMapperClassName()
        );
        methodBuild.addCode("\t\tfor ($T _row : queryResult.getRows()) {\n", ClassName.get(JsonObject.class));
        methodBuild.addCode("\t\t\tvalues.add(mapper.map(_row));\n");
        methodBuild.addCode("\t\t}\n");
        methodBuild.addCode("\t\tpromise.complete($T.ofNullable(values.stream()));\n", ClassName.get(Optional.class));
        methodBuild.addCode("\t\treturn;\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\tpromise.complete($T.empty());\n", ClassName.get(Optional.class));
        methodBuild.addCode("});\n");
        methodBuild.addCode("return promise.future();\n");

        return methodBuild;
    }

}
