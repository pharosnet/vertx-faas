package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.*;
import io.vertx.codegen.format.CamelCase;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.database.codegen.DatabaseType;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.time.*;
import java.util.List;

public class TableGenerator {

    public TableGenerator(ProcessingEnvironment processingEnv, DatabaseType databaseType) {
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.typeUtils = processingEnv.getTypeUtils();
        this.databaseType = databaseType;
    }

    private final Messager messager;
    private final Filer filer;
    private final Types typeUtils;
    private DatabaseType databaseType;

    public TableModel generate(TypeElement element) throws Exception {
        TableModel tableModel = new TableModel(this.typeUtils, element);
        this.generateMapper(tableModel);
        return tableModel;
    }

    private void generateMapper(TableModel tableModel) throws Exception {
        String pkg = tableModel.getClassName().packageName();
        ClassName mapperClassName = tableModel.getMapperClassName();

        // map
        MethodSpec.Builder mapMethod = MethodSpec.methodBuilder("map")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("io.vertx.core.json", "JsonObject"), "row")
                .returns(tableModel.getClassName())
                .addCode("if (row == null) {\n")
                .addCode("\treturn null;\n")
                .addCode("}\n")
                .addCode("$T value = new $T();\n", tableModel.getClassName(), tableModel.getClassName());
        for (ColumnModel columnModel : tableModel.getColumnModels()) {
            TypeName typeName = TypeName.get(columnModel.getElement().asType());
            if (typeName.equals(TypeName.get(String.class))) {
                mapMethod.addCode(String.format("value.set%s(row.getString(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getColumn().name()
                ));
            } else if (typeName.equals(TypeName.INT.box())) {
                mapMethod.addCode(String.format("value.set%s(row.getInteger(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getColumn().name()
                ));
            } else if (typeName.equals(TypeName.LONG.box())) {
                mapMethod.addCode(String.format("value.set%s(row.getLong(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getColumn().name()
                ));
            } else if (typeName.equals(TypeName.FLOAT.box())) {
                mapMethod.addCode(String.format("value.set%s(row.getFloat(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getColumn().name()
                ));
            } else if (typeName.equals(TypeName.DOUBLE.box())) {
                mapMethod.addCode(String.format("value.set%s(row.getDouble(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getColumn().name()
                ));
            } else if (typeName.equals(TypeName.BOOLEAN.box())) {
                mapMethod.addCode(String.format("value.set%s(row.getBoolean(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getColumn().name()
                ));
            } else if (typeName.equals(ClassName.get(Instant.class))) {
                mapMethod.addCode(String.format("value.set%s(row.getInstant(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getColumn().name()
                ));
            } else if (typeName.equals(ClassName.get(JsonObject.class))) {
                mapMethod.addCode(String.format("value.set%s(row.getJsonObject(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getColumn().name()
                ));
            } else if (typeName.equals(ClassName.get(JsonArray.class))) {
                mapMethod.addCode(String.format("value.set%s(row.getJsonArray(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getColumn().name()
                ));
            } else if (typeName.equals(ClassName.get(LocalDateTime.class))) {
                mapMethod.addCode(String.format("$T %s = row.getString(\"%s\");\n",
                        columnModel.getFieldName(),
                        columnModel.getColumn().name()
                ), ClassName.get(String.class));
                mapMethod.addCode(String.format("if (%s != null) {\n", columnModel.getFieldName()));
                mapMethod.addCode(String.format("\tvalue.set%s($T.parse(%s));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getFieldName()
                ), ClassName.get(LocalDateTime.class));
                mapMethod.addCode("}\n");
            } else if (typeName.equals(ClassName.get(LocalDate.class))) {
                mapMethod.addCode(String.format("$T %s = row.getString(\"%s\");\n",
                        columnModel.getFieldName(),
                        columnModel.getColumn().name()
                ), ClassName.get(String.class));
                mapMethod.addCode(String.format("if (%s != null) {\n", columnModel.getFieldName()));
                mapMethod.addCode(String.format("\tvalue.set%s($T.parse(%s));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getFieldName()
                ), ClassName.get(LocalDate.class));
                mapMethod.addCode("}\n");
            } else if (typeName.equals(ClassName.get(OffsetDateTime.class))) {
                mapMethod.addCode(String.format("$T %s = row.getString(\"%s\");\n",
                        columnModel.getFieldName(),
                        columnModel.getColumn().name()
                ), ClassName.get(String.class));
                mapMethod.addCode(String.format("if (%s != null) {\n", columnModel.getFieldName()));
                mapMethod.addCode(String.format("\tvalue.set%s($T.parse(%s));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getFieldName()
                ), ClassName.get(OffsetDateTime.class));
                mapMethod.addCode("}\n");
            } else if (typeName.equals(ClassName.get(ZonedDateTime.class))) {
                mapMethod.addCode(String.format("$T %s = row.getString(\"%s\");\n",
                        columnModel.getFieldName(),
                        columnModel.getColumn().name()
                ), ClassName.get(String.class));
                mapMethod.addCode(String.format("if (%s != null) {\n", columnModel.getFieldName()));
                mapMethod.addCode(String.format("\tvalue.set%s($T.parse(%s));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getFieldName()
                ), ClassName.get(ZonedDateTime.class));
                mapMethod.addCode("}\n");
            } else if (typeName.equals(ClassName.get(Duration.class))) {
                mapMethod.addCode(String.format("$T %s = row.getString(\"%s\");\n",
                        columnModel.getFieldName(),
                        columnModel.getColumn().name()
                ), ClassName.get(String.class));
                mapMethod.addCode(String.format("if (%s != null) {\n", columnModel.getFieldName()));
                mapMethod.addCode(String.format("\tvalue.set%s($T.parse(%s));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getFieldName()
                ), ClassName.get(Duration.class));
                mapMethod.addCode("}\n");
            } else if (this.typeUtils.asElement(columnModel.getElement().asType()).getKind().equals(ElementKind.ENUM)) {
                mapMethod.addCode(String.format("$T %s = row.getString(\"%s\");\n",
                        columnModel.getFieldName(),
                        columnModel.getColumn().name()
                ), ClassName.get(String.class));
                mapMethod.addCode(String.format("if (%s != null) {\n", columnModel.getFieldName()));
                mapMethod.addCode(String.format("\tvalue.set%s($T.valueOf(%s));\n",
                        CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                        columnModel.getFieldName()
                ), columnModel.getClassName());
                mapMethod.addCode("}\n");
            } else {
                throw new Exception("未知类型" + columnModel.getClassName().toString());
            }
        }
        mapMethod.addCode("return value;");

        // class
        TypeSpec typeBuilder = TypeSpec.classBuilder(mapperClassName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(mapMethod.build())
                .build();

        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder)
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
                .indent("\t")
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, mapperClassName.simpleName()));
    }


}
