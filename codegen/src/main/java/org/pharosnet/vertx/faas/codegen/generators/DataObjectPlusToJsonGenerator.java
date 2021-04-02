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
import org.pharosnet.vertx.faas.codegen.http.HttpMethod;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class DataObjectPlusToJsonGenerator {

    public DataObjectPlusToJsonGenerator(Elements elementUtils, Types typeUtils) {
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
    }

    private Elements elementUtils;
    private Types typeUtils;

    public MethodSpec.Builder generate(TypeElement typeElement, List<VariableElement> fieldElements) throws Exception {
        MethodSpec.Builder toMethod = MethodSpec.methodBuilder("toJson")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ClassName.get(typeElement.asType()), "value")
                .addParameter(ClassName.get(JsonObject.class), "jsonObject")
                .returns(ClassName.VOID);


        toMethod.addCode("if (value == null) {\n");
        toMethod.addCode("\treturn;\n");
        toMethod.addCode("}\n");
        toMethod.addCode("\n");

        for (VariableElement fieldElement : fieldElements) {
            toMethod.addCode(String.format("if (value.get%s() != null) {\n", CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString()))));
            if (this.typeUtils.asElement(fieldElement.asType()).getAnnotation(DataObject.class) != null) {

                toMethod.addCode(String.format("\tjsonObject.put(\"%s\", value.get%s().toJson());\n",
                        fieldElement.getSimpleName().toString(),
                        CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString()))
                ));

            } else {
                TypeName typeName = TypeName.get(fieldElement.asType());
                if (!(typeName instanceof ParameterizedTypeName)) {
                    // common
                    if (typeName.equals(TypeName.get(LocalDateTime.class))) {
                        toMethod.addCode(String.format("jsonObject.put(\"%s\", value.get%s().toString());\n",
                                fieldElement.getSimpleName().toString(),
                                CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString()))
                        ));
                    } else if (typeName.equals(TypeName.get(LocalDate.class))) {
                        toMethod.addCode(String.format("jsonObject.put(\"%s\", value.get%s().toString());\n",
                                fieldElement.getSimpleName().toString(),
                                CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString()))
                        ));
                    } else if (typeName.equals(TypeName.get(OffsetDateTime.class))) {
                        toMethod.addCode(String.format("jsonObject.put(\"%s\", value.get%s().toString());\n",
                                fieldElement.getSimpleName().toString(),
                                CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString()))
                        ));
                    } else if (typeName.equals(TypeName.get(ZonedDateTime.class))) {
                        toMethod.addCode(String.format("jsonObject.put(\"%s\", value.get%s().toString());\n",
                                fieldElement.getSimpleName().toString(),
                                CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString()))
                        ));
                    } else if (typeName.equals(TypeName.get(Duration.class))) {
                        toMethod.addCode(String.format("jsonObject.put(\"%s\", value.get%s().toString());\n",
                                fieldElement.getSimpleName().toString(),
                                CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString()))
                        ));
                    } else if (typeName.equals(TypeName.get(JsonObject.class)) || typeName.equals(TypeName.get(JsonArray.class))) {
                        toMethod.addCode(String.format("jsonObject.put(\"%s\", value.get%s().encode());\n",
                                fieldElement.getSimpleName().toString(),
                                CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString()))
                        ));
                    } else {
                        toMethod.addCode(String.format("jsonObject.put(\"%s\", value.get%s());\n",
                                fieldElement.getSimpleName().toString(),
                                CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString()))
                        ));
                    }

                } else {
                    // list
                    ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) typeName;
                    if (!parameterizedTypeName.rawType.equals(ClassName.get(List.class))) {
                        throw new Exception("只支持List.");
                    }


                    toMethod.addCode(String.format("\t$T %sArray = new $T();\n",
                            fieldElement.getSimpleName().toString()
                            ),
                            ClassName.get(JsonArray.class),
                            ClassName.get(JsonArray.class)
                    );

                    this.generateListFieldCode(
                            toMethod, parameterizedTypeName,
                            fieldElement.getSimpleName().toString(),
                            String.format("value.get%s()",
                                    CamelCase.INSTANCE.format(List.of(fieldElement.getSimpleName().toString()))
                            ),
                            0);

                    toMethod.addCode(String.format("\tjsonObject.put(\"%s\", %sArray);\n",
                            fieldElement.getSimpleName().toString(),
                            fieldElement.getSimpleName().toString()
                    ));

                }
            }
            toMethod.addCode("}\n");

        }

        return toMethod;
    }

    private void generateListFieldCode(
            MethodSpec.Builder toMethod, ParameterizedTypeName parameterizedTypeName,
            String valuesName,
            String leafArrayName, int layer)
            throws Exception {

        TypeName argTypeName = parameterizedTypeName.typeArguments.get(0);

        toMethod.addCode(String.format("\t%sfor ($T %sItem : %s) {\n",
                Strings.appendBefore("", "\t", layer),
                Strings.appendBefore(
                        valuesName, "_", layer
                ),
                leafArrayName
                ),
                argTypeName
        );
        toMethod.addCode(String.format("\t\t%sif (%sItem == null) {\n",
                Strings.appendBefore("", "\t", layer),
                Strings.appendBefore(
                        valuesName, "_", layer
                )
        ));
        toMethod.addCode(String.format("\t\t\t%scontinue;\n", Strings.appendBefore("", "\t", layer)));
        toMethod.addCode(String.format("\t\t%s}\n", Strings.appendBefore("", "\t", layer)));


        TypeName typeArgTypeName = parameterizedTypeName.typeArguments.get(0);
        if (typeArgTypeName instanceof ParameterizedTypeName) {
            // list
            parameterizedTypeName = (ParameterizedTypeName) typeArgTypeName;
            if (!parameterizedTypeName.rawType.equals(ClassName.get(List.class))) {
                throw new Exception("只支持List.");
            }
            toMethod.addCode(String.format("\t\t%s$T %sArray = new $T();\n",
                    Strings.appendBefore("", "\t", layer),
                    Strings.appendBefore(
                            valuesName, "_", layer + 1)
                    ),
                    ClassName.get(JsonArray.class),
                    ClassName.get(JsonArray.class)
            );

            this.generateListFieldCode(toMethod, parameterizedTypeName, valuesName,
                    Strings.appendBefore(
                            valuesName+"Item", "_", layer
                    ), layer + 1);

            toMethod.addCode(String.format("\t\t%s%sArray.add(%sArray);\n",
                    Strings.appendBefore("", "\t", layer),
                    Strings.appendBefore(
                            valuesName, "_", layer
                    ),
                    Strings.appendBefore(
                            valuesName, "_", layer + 1
                    )
            ));

        } else {

            if (typeArgTypeName.equals(TypeName.get(LocalDateTime.class))) {
                toMethod.addCode(String.format("\t\t%s%sArray.add(%sItem().toString());\n",
                        Strings.appendBefore("", "\t", layer),
                        Strings.appendBefore(
                                valuesName, "_", layer
                        ),
                        Strings.appendBefore(
                                valuesName, "_", layer
                        )
                ));
            } else if (typeArgTypeName.equals(TypeName.get(LocalDate.class))) {
                toMethod.addCode(String.format("\t\t%s%sArray.add(%sItem().toString());\n",
                        Strings.appendBefore("", "\t", layer),
                        Strings.appendBefore(
                                valuesName, "_", layer
                        ),
                        Strings.appendBefore(
                                valuesName, "_", layer
                        )
                ));
            } else if (typeArgTypeName.equals(TypeName.get(OffsetDateTime.class))) {
                toMethod.addCode(String.format("\t\t%s%sArray.add(%sItem().toString());\n",
                        Strings.appendBefore("", "\t", layer),
                        Strings.appendBefore(
                                valuesName, "_", layer
                        ),
                        Strings.appendBefore(
                                valuesName, "_", layer
                        )
                ));
            } else if (typeArgTypeName.equals(TypeName.get(ZonedDateTime.class))) {
                toMethod.addCode(String.format("\t\t%s%sArray.add(%sItem().toString());\n",
                        Strings.appendBefore("", "\t", layer),
                        Strings.appendBefore(
                                valuesName, "_", layer
                        ),
                        Strings.appendBefore(
                                valuesName, "_", layer
                        )
                ));
            } else if (typeArgTypeName.equals(TypeName.get(Duration.class))) {
                toMethod.addCode(String.format("\t\t%s%sArray.add(%sItem().toString());\n",
                        Strings.appendBefore("", "\t", layer),
                        Strings.appendBefore(
                                valuesName, "_", layer
                        ),
                        Strings.appendBefore(
                                valuesName, "_", layer
                        )
                ));
            } else {
                if (typeArgTypeName.isPrimitive() || typeArgTypeName.isBoxedPrimitive() || typeArgTypeName.equals(TypeName.get(String.class))) {
                    toMethod.addCode(String.format("\t\t%s%sArray.add(%sItem);\n",
                            Strings.appendBefore("", "\t", layer),
                            Strings.appendBefore(
                                    valuesName, "_", layer
                            ),
                            Strings.appendBefore(
                                    valuesName, "_", layer
                            )
                    ));
                } else {
                    if (!(typeArgTypeName instanceof ClassName)) {
                        throw new Exception("未知类型" + typeArgTypeName.toString());
                    }
                    ClassName typeArgClassName = (ClassName) typeArgTypeName;
                    TypeElement typeArgTypeElement = this.elementUtils.getTypeElement(typeArgClassName.packageName() + "." + typeArgClassName.simpleName());
                    if (typeArgTypeElement.getKind().equals(ElementKind.ENUM)) {
                        // enum
                        toMethod.addCode(String.format("\t\t%s%sArray.add(%sItem);\n",
                                Strings.appendBefore("", "\t", layer),
                                Strings.appendBefore(
                                        valuesName, "_", layer
                                ),
                                Strings.appendBefore(
                                        valuesName, "_", layer
                                )
                        ));
                    } else if (typeArgTypeElement.getAnnotation(DataObject.class) != null) {
                        // class
                        toMethod.addCode(String.format("\t\t%s%sArray.add(%sItem.toJson());\n",
                                Strings.appendBefore("", "\t", layer),
                                Strings.appendBefore(
                                        valuesName, "_", layer
                                ),
                                Strings.appendBefore(
                                        valuesName, "_", layer
                                )
                        ));
                    } else {
                        throw new Exception("未知类型" + typeArgTypeName.toString());
                    }
                }
            }
        }
        toMethod.addCode(String.format("\t%s}\n", Strings.appendBefore("", "\t", layer)));


    }

}
