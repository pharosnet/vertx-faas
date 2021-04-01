package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.*;
import io.vertx.codegen.format.CamelCase;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import org.pharosnet.vertx.faas.database.codegen.DatabaseType;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class DALInsertGenerator {

    public DALInsertGenerator(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    private final DatabaseType databaseType;

    public void generate(DALModel dalModel, TypeSpec.Builder typeBuilder) {

        String sql = this.generateSQL(dalModel.getTableModel());

        // sql
        FieldSpec.Builder staticSqlField = FieldSpec.builder(
                ClassName.get(String.class), "_insertSQL",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("$S", sql);
        typeBuilder.addField(staticSqlField.build());

        typeBuilder.addMethod(this.generateOne(dalModel).build());
        typeBuilder.addMethod(this.generateBatch(dalModel).build());

    }

    public String generateSQL(TableModel tableModel) {
        StringBuilder builder = new StringBuilder();
        builder.append("INSERT INTO").append(" ");
        if (tableModel.getTable().schema().length() > 0) {
            builder.append(tableModel.getTable().schema().toUpperCase()).append(".");
        }
        builder.append(tableModel.getTable().name()).append(" ");
        StringBuilder columns = new StringBuilder();
        StringBuilder args = new StringBuilder();
        int pos = 1;
        for (ColumnModel columnModel : tableModel.getColumnModels()) {
            columns.append(", ").append(columnModel.getColumn().name());
            if (databaseType.equals(DatabaseType.MYSQL)) {
                args.append(", ?");
            } else {
                args.append(", $").append(pos);
                pos++;
            }
        }
        builder.append("(").append(columns.toString().substring(2)).append(")");
        builder.append(" ").append("VALUES").append(" ").append("(").append(columns.toString().substring(2)).append(")");
        return builder.toString().toUpperCase();
    }

    public MethodSpec.Builder generateOne(DALModel dalModel) {
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder("insert")
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

        for (ColumnModel columnModel : dalModel.getTableModel().getColumnModels()) {
            methodBuild.addCode(String.format("args.add(row.get%s());\n", CamelCase.INSTANCE.format(List.of(columnModel.getFieldName()))));
        }
        methodBuild
                .addCode("$T arg = new $T();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"), ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"))
                .addCode("arg.setQuery(_insertSQL);\n")
                .addCode("arg.setArgs(args);\n")
                .addCode("arg.setBatch(false);\n")
                .addCode("arg.setSlaverMode(false);\n")
                .addCode(String.format("arg.setNeedLastInsertedId(%b);\n", dalModel.getTableModel().isNeedLastInsertedId()));

        methodBuild.addCode("this.service.query(context, arg, r -> {\n");
        methodBuild.addCode("\tif (r.failed()) {\n");
        methodBuild.addCode("\t\tlog.error(\"insert failed\", r.cause());\n");
        methodBuild.addCode("\t\tpromise.fail(r.cause());\n");
        methodBuild.addCode("\t\treturn;\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\tQueryResult queryResult = r.result();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryResult"));
        methodBuild.addCode("\tif (log.isDebugEnabled()) {\n");
        methodBuild.addCode("\t\tlog.debug(\"insert succeed, affected = {} latency = {}\", queryResult.getAffected(), queryResult.getLatency());\n");
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
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder("insert")
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
                                ClassName.get(Stream.class),
                                dalModel.getTableClassName()
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
        for (ColumnModel columnModel : dalModel.getTableModel().getColumnModels()) {
            methodBuild.addCode(String.format("\t\targ.add(row.get%s());\n", CamelCase.INSTANCE.format(List.of(columnModel.getFieldName()))));
        }
        methodBuild.addCode("\t\treturn arg;\n");
        methodBuild.addCode("\t}).collect(Collectors.toList()));\n");
        methodBuild.addCode("\t\n");
        methodBuild
                .addCode("$T arg = new $T();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"), ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"))
                .addCode("arg.setQuery(_insertSQL);\n")
                .addCode("arg.setArgs(args);\n")
                .addCode("arg.setBatch(true);\n")
                .addCode("arg.setSlaverMode(false);\n")
                .addCode(String.format("arg.setNeedLastInsertedId(%b);\n", dalModel.getTableModel().isNeedLastInsertedId()));

        methodBuild.addCode("this.service.query(context, arg, r -> {\n");
        methodBuild.addCode("\tif (r.failed()) {\n");
        methodBuild.addCode("\t\tlog.error(\"insert batch failed\", r.cause());\n");
        methodBuild.addCode("\t\tpromise.fail(r.cause());\n");
        methodBuild.addCode("\t\treturn;\n");
        methodBuild.addCode("\t}\n");
        methodBuild.addCode("\tQueryResult queryResult = r.result();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryResult"));
        methodBuild.addCode("\tif (log.isDebugEnabled()) {\n");
        methodBuild.addCode("\t\tlog.debug(\"insert batch succeed, affected = {} latency = {}\", queryResult.getAffected(), queryResult.getLatency());\n");
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
