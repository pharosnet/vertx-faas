package org.pharosnet.vertx.faas.codegen.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.format.CamelCase;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.codegen.commons.Strings;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class DataObjectPlusFromJsonGenerator {

    public DataObjectPlusFromJsonGenerator(Elements elementUtils, Types typeUtils) {
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
    }

    private Elements elementUtils;
    private Types typeUtils;

    public MethodSpec.Builder generate(TypeElement typeElement, List<VariableElement> fieldElements) throws Exception {
        MethodSpec.Builder fromMethod = MethodSpec.methodBuilder("fromJson")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ClassName.get(JsonObject.class), "jsonObject")
                .addParameter(ClassName.get(typeElement.asType()), "value")
                .returns(ClassName.VOID);


        fromMethod.addCode("if (jsonObject == null) {\n");
        fromMethod.addCode("\treturn;\n");
        fromMethod.addCode("}\n");
        fromMethod.addCode("\n");

        for (VariableElement fieldElement : fieldElements) {
            TypeName typeName = TypeName.get(fieldElement.asType());
            if (typeName.equals(TypeName.get(String.class))) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getString(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(TypeName.INT)) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getInteger(\"%s\", 0));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(TypeName.INT.box())) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getInteger(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(TypeName.LONG)) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getLong(\"%s\", 0L));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(TypeName.LONG.box())) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getLong(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(TypeName.FLOAT)) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getFloat(\"%s\", 0.0F));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(TypeName.FLOAT.box())) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getFloat(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(TypeName.DOUBLE)) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getDouble(\"%s\", 0.0D));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(TypeName.DOUBLE.box())) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getDouble(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(TypeName.BOOLEAN)) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getBoolean(\"%s\", false));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(TypeName.BOOLEAN.box())) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getBoolean(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(ClassName.get(Instant.class))) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getInstant(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(ClassName.get(JsonObject.class))) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getJsonObject(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(ClassName.get(JsonArray.class))) {
                fromMethod.addCode(String.format("value.set%s(jsonObject.getJsonArray(\"%s\"));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ));
            } else if (typeName.equals(ClassName.get(LocalDateTime.class))) {
                fromMethod.addCode(String.format("$T %s = jsonObject.getString(\"%s\");\n",
                        fieldElement.getSimpleName().toString(),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(String.class));
                fromMethod.addCode(String.format("if (%s != null) {\n", fieldElement.getSimpleName().toString()));
                fromMethod.addCode(String.format("\tvalue.set%s($T.parse(%s));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(LocalDateTime.class));
                fromMethod.addCode("}\n");
            } else if (typeName.equals(ClassName.get(LocalDate.class))) {
                fromMethod.addCode(String.format("$T %s = jsonObject.getString(\"%s\");\n",
                        fieldElement.getSimpleName().toString(),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(String.class));
                fromMethod.addCode(String.format("if (%s != null) {\n", fieldElement.getSimpleName().toString()));
                fromMethod.addCode(String.format("\tvalue.set%s($T.parse(%s));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(LocalDate.class));
                fromMethod.addCode("}\n");
            } else if (typeName.equals(ClassName.get(OffsetDateTime.class))) {
                fromMethod.addCode(String.format("$T %s = jsonObject.getString(\"%s\");\n",
                        fieldElement.getSimpleName().toString(),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(String.class));
                fromMethod.addCode(String.format("if (%s != null) {\n", fieldElement.getSimpleName().toString()));
                fromMethod.addCode(String.format("\tvalue.set%s($T.parse(%s));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(OffsetDateTime.class));
                fromMethod.addCode("}\n");
            } else if (typeName.equals(ClassName.get(ZonedDateTime.class))) {
                fromMethod.addCode(String.format("$T %s = jsonObject.getString(\"%s\");\n",
                        fieldElement.getSimpleName().toString(),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(String.class));
                fromMethod.addCode(String.format("if (%s != null) {\n", fieldElement.getSimpleName().toString()));
                fromMethod.addCode(String.format("\tvalue.set%s($T.parse(%s));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(ZonedDateTime.class));
                fromMethod.addCode("}\n");
            } else if (typeName.equals(ClassName.get(Duration.class))) {
                fromMethod.addCode(String.format("$T %s = jsonObject.getString(\"%s\");\n",
                        fieldElement.getSimpleName().toString(),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(String.class));
                fromMethod.addCode(String.format("if (%s != null) {\n", fieldElement.getSimpleName().toString()));
                fromMethod.addCode(String.format("\tvalue.set%s($T.parse(%s));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(Duration.class));
                fromMethod.addCode("}\n");
            } else if (this.typeUtils.asElement(fieldElement.asType()).getKind().equals(ElementKind.ENUM)) {
                fromMethod.addCode(String.format("value.set%s($T.valueOf(jsonObject.getString(\"%s\")));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(fieldElement.asType()));
            } else if (this.typeUtils.asElement(fieldElement.asType()).getAnnotation(DataObject.class) != null) {
                // class
                fromMethod.addCode(String.format("$T %sJsonObject = jsonObject.getJsonObject(\"%s\");\n",
                        fieldElement.getSimpleName().toString(),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(JsonObject.class));
                fromMethod.addCode(String.format("if (%sJsonObject != null) {\n", fieldElement.getSimpleName().toString()));
                fromMethod.addCode(String.format("\tvalue.set%s(new $T(%sJsonObject));\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(fieldElement.asType()));
                fromMethod.addCode("}\n");
            } else if (typeName instanceof ParameterizedTypeName) {
                // list
                ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) typeName;
                if (!parameterizedTypeName.rawType.equals(ClassName.get(List.class))) {
                    throw new Exception("只支持List.");
                }
                fromMethod.addCode(String.format("$T %sArray = jsonObject.getJsonArray(\"%s\");\n",
                        fieldElement.getSimpleName().toString(),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(JsonArray.class));


                fromMethod.addCode(String.format("if (%sArray != null) {\n", fieldElement.getSimpleName().toString()));

                ///
                this.generateListFieldCode(
                        fromMethod, parameterizedTypeName,
                        fieldElement.getSimpleName().toString(), 0);

//                fromMethod.addCode("\t}\n");
                fromMethod.addCode(String.format("\tvalue.set%s(%s);\n",
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString())),
                        fieldElement.getSimpleName().toString()
                ), ClassName.get(fieldElement.asType()));

                fromMethod.addCode("}\n");

            } else {
                throw new Exception("未知类型" + ClassName.get(fieldElement.asType()).toString());
            }
        }

        return fromMethod;
    }

    private void generateListFieldCode(
            MethodSpec.Builder fromMethod, ParameterizedTypeName parameterizedTypeName,
            String fieldName, int layer)
            throws Exception {

        fromMethod.addCode(String.format("\t%s$T %s = new $T<>();\n",
                Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                Strings.appendBefore(fieldName, "_", layer)
                ),
                parameterizedTypeName,
                ClassName.get(ArrayList.class)
        );
        fromMethod.addCode(String.format("\t%sfor (int %s = 0; %s < %sArray.size(); %s ++) {\n",
                Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                Strings.appendBefore("i", "_", layer),
                Strings.appendBefore("i", "_", layer),
                Strings.appendBefore(fieldName, "_", layer),
                Strings.appendBefore("i", "_", layer)
        ));

        TypeName typeArgTypeName = parameterizedTypeName.typeArguments.get(0);
        if (typeArgTypeName instanceof ParameterizedTypeName) {

            parameterizedTypeName = (ParameterizedTypeName) typeArgTypeName;
            if (!parameterizedTypeName.rawType.equals(ClassName.get(List.class))) {
                throw new Exception("只支持List.");
            }

            fromMethod.addCode(String.format("\t\t%s$T %sArray = %sArray.getJsonArray(%s);\n",
                    Strings.appendBefore("", "\t", layer),
                    Strings.appendBefore(fieldName, "_", layer + 1),
                    fieldName,
                    Strings.appendBefore("i", "_", layer)
                    ),
                    ClassName.get(JsonArray.class)
            );

            fromMethod.addCode(String.format("\t\t%sif (%sArray != null) {\n",
                    Strings.appendBefore("", "\t", layer),
                    Strings.appendBefore(fieldName, "_", layer + 1)
                    )
            );

            this.generateListFieldCode(fromMethod, parameterizedTypeName,
                    fieldName, layer + 1
            );


            fromMethod.addCode(String.format("\t\t\t%s%s.add(%s);\n",
                    Strings.appendBefore("", "\t", layer),
                    Strings.appendBefore(fieldName, "_", layer),
                    Strings.appendBefore(fieldName, "_", layer + 1)
                    )
            );

            fromMethod.addCode(String.format("\t\t%s}\n",
                    Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                    )
            );

        } else {

            if (typeArgTypeName.equals(TypeName.get(String.class))) {
                fromMethod.addCode(String.format("\t\t%s$T object = %sArray.getString(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(String.class)
                );
            } else if (typeArgTypeName.equals(TypeName.INT) || typeArgTypeName.equals(TypeName.INT.box())) {
                fromMethod.addCode(String.format("\t\t%s$T object = %sArray.getInteger(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(Integer.class)
                );
            } else if (typeArgTypeName.equals(TypeName.LONG) || typeArgTypeName.equals(TypeName.LONG.box())) {
                fromMethod.addCode(String.format("\t\t%s$T object = %sArray.getLong(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(Long.class)
                );
            } else if (typeArgTypeName.equals(TypeName.FLOAT) || typeArgTypeName.equals(TypeName.FLOAT.box())) {
                fromMethod.addCode(String.format("\t\t%s$T object = %sArray.getFloat(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(Float.class)
                );
            } else if (typeArgTypeName.equals(TypeName.DOUBLE) || typeArgTypeName.equals(TypeName.DOUBLE.box())) {
                fromMethod.addCode(String.format("\t\t%s$T object = %sArray.getDouble(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(Double.class)
                );
            } else if (typeArgTypeName.equals(TypeName.BOOLEAN) || typeArgTypeName.equals(TypeName.BOOLEAN.box())) {
                fromMethod.addCode(String.format("\t\t%s$T object = %sArray.getBoolean(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(Boolean.class)
                );
            } else if (typeArgTypeName.equals(ClassName.get(Instant.class))) {
                fromMethod.addCode(String.format("\t\t%s$T object = %sArray.getInstant(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(Instant.class)
                );
            } else if (typeArgTypeName.equals(ClassName.get(JsonObject.class))) {
                fromMethod.addCode(String.format("\t\t%s$T object = %sArray.getJsonObject(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(JsonObject.class)
                );
            } else if (typeArgTypeName.equals(ClassName.get(JsonArray.class))) {
                fromMethod.addCode(String.format("\t\t%s$T object = %sArray.getJsonArray(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(JsonArray.class)
                );
            } else if (typeArgTypeName.equals(ClassName.get(LocalDateTime.class))) {
                fromMethod.addCode(String.format("\t\t%s$T object = null;\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                        ),
                        ClassName.get(LocalDateTime.class)
                );
                fromMethod.addCode(String.format("\t\t%s$T _object = %sArray.getString(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(String.class)
                );
                fromMethod.addCode(String.format("\t\t%sif (_object != null) {\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ));
                fromMethod.addCode(String.format("\t\t\t%sobject = $T.parse(_object));\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ), ClassName.get(LocalDateTime.class));
                fromMethod.addCode(String.format("\t\t%s}\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ));
            } else if (typeArgTypeName.equals(ClassName.get(LocalDate.class))) {
                fromMethod.addCode(String.format("\t\t%s$T object = null;\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                        ),
                        ClassName.get(LocalDate.class)
                );
                fromMethod.addCode(String.format("\t\t%s$T _object = %sArray.getString(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(String.class)
                );
                fromMethod.addCode(String.format("\t\t%sif (_object != null) {\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ));
                fromMethod.addCode(String.format("\t\t\t%sobject = $T.parse(_object));\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ), ClassName.get(LocalDate.class));
                fromMethod.addCode(String.format("\t\t%s}\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ));
            } else if (typeArgTypeName.equals(ClassName.get(OffsetDateTime.class))) {
                fromMethod.addCode(String.format("\t\t%s$T object = null;\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                        ),
                        ClassName.get(OffsetDateTime.class)
                );
                fromMethod.addCode(String.format("\t\t%s$T _object = %sArray.getString(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(String.class)
                );
                fromMethod.addCode(String.format("\t\t%sif (_object != null) {\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ));
                fromMethod.addCode(String.format("\t\t\t%sobject = $T.parse(_object));\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ), ClassName.get(OffsetDateTime.class));
                fromMethod.addCode(String.format("\t\t%s}\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ));
            } else if (typeArgTypeName.equals(ClassName.get(ZonedDateTime.class))) {
                fromMethod.addCode(String.format("\t\t%s$T object = null;\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                        ),
                        ClassName.get(ZonedDateTime.class)
                );
                fromMethod.addCode(String.format("\t\t%s$T _object = %sArray.getString(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(String.class)
                );
                fromMethod.addCode(String.format("\t\t%sif (_object != null) {\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ));
                fromMethod.addCode(String.format("\t\t\t%sobject = $T.parse(_object));\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ), ClassName.get(ZonedDateTime.class));
                fromMethod.addCode(String.format("\t\t%s}\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ));
            } else if (typeArgTypeName.equals(ClassName.get(Duration.class))) {
                fromMethod.addCode(String.format("\t\t%s$T object = null;\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                        ),
                        ClassName.get(Duration.class)
                );
                fromMethod.addCode(String.format("\t\t%s$T _object = %sArray.getString(%s);\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                        Strings.appendBefore(fieldName, "_", layer),
                        Strings.appendBefore("i", "_", layer)
                        ),
                        ClassName.get(String.class)
                );
                fromMethod.addCode(String.format("\t\t%sif (_object != null) {\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ));
                fromMethod.addCode(String.format("\t\t\t%sobject = $T.parse(_object));\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ), ClassName.get(Duration.class));
                fromMethod.addCode(String.format("\t\t%s}\n",
                        Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                ));
            } else {
                if (!(typeArgTypeName instanceof ClassName)) {
                    throw new Exception("未知类型" + typeArgTypeName.toString());
                }
                ClassName typeArgClassName = (ClassName) typeArgTypeName;
                TypeElement typeArgTypeElement = this.elementUtils.getTypeElement(typeArgClassName.packageName() + "." + typeArgClassName.simpleName());
                // enum
                if (typeArgTypeElement.getKind().equals(ElementKind.ENUM)) {
                    fromMethod.addCode(String.format("\t\t%s$T object = $T.valueOf(%sArray.getString(%s));\n",
                            Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                            Strings.appendBefore(fieldName, "_", layer),
                            Strings.appendBefore("i", "_", layer)
                            ),
                            typeArgClassName,
                            typeArgClassName
                    );
                } else if (typeArgTypeElement.getAnnotation(DataObject.class) != null) {
                    // class
                    fromMethod.addCode(String.format("\t\t%s$T object = null;\n",
                            Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                            ),
                            typeArgClassName
                    );
                    fromMethod.addCode(String.format("\t\t%s$T _object = %sArray.getJsonObject(%s);\n",
                            Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                            Strings.appendBefore(fieldName, "_", layer),
                            Strings.appendBefore("i", "_", layer)
                            ),
                            ClassName.get(JsonObject.class)
                    );
                    fromMethod.addCode(String.format("\t\t%sif (_object != null) {\n",
                            Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                    ));
                    fromMethod.addCode(String.format("\t\t\t%sobject = new $T(_object);\n",
                            Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                    ), typeArgClassName);
                    fromMethod.addCode(String.format("\t\t%s}\n",
                            Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
                    ));
                } else {
                    throw new Exception("未知类型" + typeArgTypeName.toString());
                }
            }

            fromMethod.addCode(String.format("\t\t%s%s.add(object);\n",
                    Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1),
                    Strings.appendBefore(fieldName, "_", layer)
            ));
        }

        fromMethod.addCode(String.format("\t%s}\n",
                Strings.appendBefore("", "\t", layer == 0 ? 0 : layer + 1)
        ));

    }

}
