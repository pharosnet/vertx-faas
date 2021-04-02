package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.*;
import io.vertx.codegen.format.CamelCase;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.database.codegen.DatabaseType;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DALUpdateGenerator {

    public DALUpdateGenerator(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    private final DatabaseType databaseType;

    public void generate(DALModel dalModel, TypeSpec.Builder typeBuilder) {

        String sql = this.generateSQL(dalModel.getTableModel());

        // sql
        FieldSpec.Builder staticSqlField = FieldSpec.builder(
                ClassName.get(String.class), "_updateSQL",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("$S", sql);
        typeBuilder.addField(staticSqlField.build());

        typeBuilder.addMethod(this.generateOne(dalModel).build());
        typeBuilder.addMethod(this.generateBatch(dalModel).build());

    }

    public String generateSQL(TableModel tableModel) {
        StringBuilder builder = new StringBuilder();
        builder.append("UPDATE").append(" ");
        if (tableModel.getTable().schema().length() > 0) {
            builder.append(tableModel.getTable().schema().toUpperCase()).append(".");
        }
        builder.append(tableModel.getTable().name()).append(" ");
        builder.append("SET").append(" ");

        StringBuilder columns = new StringBuilder();
        String idColumnName = "";
        String versionColumnName = "";
        int pos = 1;
        for (ColumnModel columnModel : tableModel.getColumnModels()) {
            if (columnModel.getKind().equals(ColumnKind.ID)) {
                idColumnName = columnModel.getColumn().name();
                continue;
            }
            if (columnModel.getKind().equals(ColumnKind.VERSION)) {
                versionColumnName = columnModel.getColumn().name();
                columns.append(", ")
                        .append(columnModel.getColumn().name())
                        .append(" = ")
                        .append(columnModel.getColumn().name()).append(" + 1");
                continue;
            }
            columns.append(", ").append(columnModel.getColumn().name()).append(" = ");
            if (databaseType.equals(DatabaseType.MYSQL)) {
                columns.append("?");
            } else {
                columns.append("$").append(pos);
                pos++;
            }
        }
        builder.append(columns.toString().substring(2)).append(" ");
        builder.append("WHERE").append(" ").append(idColumnName).append(" = ");
        if (databaseType.equals(DatabaseType.MYSQL)) {
            builder.append("?");
        } else {
            builder.append("$").append(pos);
        }
        if (!versionColumnName.isBlank()) {
            builder.append(" AND ").append(versionColumnName).append(" = ");
            if (databaseType.equals(DatabaseType.MYSQL)) {
                builder.append("?");
            } else {
                builder.append("$").append(pos + 1);
            }
        }
        return builder.toString().toUpperCase();
    }

    public MethodSpec.Builder generateOne(DALModel dalModel) {
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("org.pharosnet.vertx.faas.database.api", "SqlContext"), "context")
                .addParameter(dalModel.getTableClassName(), "row")
                .returns(
                        ParameterizedTypeName.get(
                                ClassName.get(Future.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(Optional.class),
                                        dalModel.getTableClassName()
                                )
                        )
                );

        methodBuild.addCode("if (row == null) {\n");
        methodBuild.addCode("\treturn Future.failedFuture(\"row is empty\");\n");
        methodBuild.addCode("}\n");

        methodBuild.addStatement("$T promise = $T.promise()",
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

        String idField = "";
        String versionField = "";
        for (ColumnModel columnModel : dalModel.getTableModel().getColumnModels()) {
            if (columnModel.getKind().equals(ColumnKind.ID)) {
                idField = columnModel.getFieldName();
                continue;
            }
            if (columnModel.getKind().equals(ColumnKind.VERSION)) {
                versionField = columnModel.getFieldName();
                continue;
            }
            methodBuild.addCode(String.format("args.add(row.get%s());\n", CamelCase.INSTANCE.format(List.of(columnModel.getFieldName()))));
        }
        if (!idField.isBlank()) {
            methodBuild.addCode(String.format("args.add(row.get%s());\n", CamelCase.INSTANCE.format(List.of(idField))));
        }
        if (!versionField.isBlank()) {
            methodBuild.addCode(String.format("args.add(row.get%s());\n", CamelCase.INSTANCE.format(List.of(versionField))));
        }

        methodBuild
                .addCode("$T arg = new $T();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"), ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"))
                .addCode("arg.setQuery(_updateSQL);\n")
                .addCode("arg.setArgs(args);\n")
                .addCode("arg.setBatch(false);\n")
                .addCode("arg.setSlaverMode(false);\n")
                .addCode("arg.setNeedLastInsertedId(false);\n");

        methodBuild.addCode("this.service().query(context, arg, r -> {\n");
        methodBuild.addCode("\tif (r.failed()) {\n");
        methodBuild.addCode("\t\tlog.error(\"update failed\", r.cause());\n");
        methodBuild.addCode("\t\tpromise.fail(r.cause());\n");
        methodBuild.addCode("\t\treturn;\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\t$T queryResult = r.result();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryResult"));
        methodBuild.addCode("\tif (log.isDebugEnabled()) {\n");
        methodBuild.addCode("\t\tlog.debug(\"update succeed, affected = {} latency = {}\", queryResult.getAffected(), queryResult.getLatency());\n");
        methodBuild.addCode("\t}\n");

        methodBuild.addCode("\n");
        methodBuild.addCode("\tif (queryResult.getAffected() == 0) {\n");
        methodBuild.addCode("\t\tpromise.complete($T.empty());\n", ClassName.get(Optional.class));
        methodBuild.addCode("\t\treturn;\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\tpromise.complete($T.ofNullable(row));\n", ClassName.get(Optional.class));
        methodBuild.addCode("});\n");
        methodBuild.addCode("return promise.future();\n");

        return methodBuild;
    }

    public MethodSpec.Builder generateBatch(DALModel dalModel) {
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("org.pharosnet.vertx.faas.database.api", "SqlContext"), "context")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Stream.class), dalModel.getTableClassName()), "rows")
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

        methodBuild.addCode("if (rows == null || rows.count() == 0) {\n");
        methodBuild.addCode("\treturn Future.failedFuture(\"rows is empty\");\n");
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

        methodBuild.addCode("$T args = new $T(\n",
                ClassName.get(JsonArray.class),
                ClassName.get(JsonArray.class)
        );

        methodBuild.addCode("\trows.map(row -> {\n");
        methodBuild.addCode("\t\tJsonArray arg = new JsonArray();\n");
        String idField = "";
        String versionField = "";
        for (ColumnModel columnModel : dalModel.getTableModel().getColumnModels()) {
            if (columnModel.getKind().equals(ColumnKind.ID)) {
                idField = columnModel.getFieldName();
                continue;
            }
            if (columnModel.getKind().equals(ColumnKind.VERSION)) {
                versionField = columnModel.getFieldName();
                continue;
            }
            methodBuild.addCode(String.format("\t\targ.add(row.get%s());\n", CamelCase.INSTANCE.format(List.of(columnModel.getFieldName()))));
        }
        if (!idField.isBlank()) {
            methodBuild.addCode(String.format("\t\targ.add(row.get%s());\n", CamelCase.INSTANCE.format(List.of(idField))));
        }
        if (!versionField.isBlank()) {
            methodBuild.addCode(String.format("\t\targ.add(row.get%s());\n", CamelCase.INSTANCE.format(List.of(versionField))));
        }
        methodBuild.addCode("\t\treturn arg;\n");
        methodBuild.addCode("\t}).collect($T.toList()));\n", ClassName.get(Collectors.class));
        methodBuild.addCode("\t\n");

        methodBuild
                .addCode("$T arg = new $T();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"), ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"))
                .addCode("arg.setQuery(_updateSQL);\n")
                .addCode("arg.setArgs(args);\n")
                .addCode("arg.setBatch(true);\n")
                .addCode("arg.setSlaverMode(false);\n")
                .addCode("arg.setNeedLastInsertedId(false);\n");

        methodBuild.addCode("this.service().query(context, arg, r -> {\n");
        methodBuild.addCode("\tif (r.failed()) {\n");
        methodBuild.addCode("\t\tlog.error(\"update batch failed\", r.cause());\n");
        methodBuild.addCode("\t\tpromise.fail(r.cause());\n");
        methodBuild.addCode("\t\treturn;\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\t$T queryResult = r.result();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryResult"));
        methodBuild.addCode("\tif (log.isDebugEnabled()) {\n");
        methodBuild.addCode("\t\tlog.debug(\"update batch succeed, affected = {} latency = {}\", queryResult.getAffected(), queryResult.getLatency());\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\n");
        methodBuild.addCode("\tif (queryResult.getAffected() != rows.count()) {\n");
        methodBuild.addCode("\t\tpromise.complete($T.empty());\n", ClassName.get(Optional.class));
        methodBuild.addCode("\t\treturn;\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\tpromise.complete($T.ofNullable(rows));\n", ClassName.get(Optional.class));
        methodBuild.addCode("});\n");
        methodBuild.addCode("return promise.future();\n");

        return methodBuild;
    }

}
