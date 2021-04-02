package org.pharosnet.vertx.faas.codegen.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.swagger.v3.oas.models.media.*;
import org.pharosnet.vertx.faas.codegen.annotation.oas.ApiModel;
import org.pharosnet.vertx.faas.codegen.annotation.oas.ApiModelProperty;

import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OASComponentGenerator {

    public OASComponentGenerator(Elements elementUtils) {
        this.elementUtils = elementUtils;
        this.schemas = new HashMap<>();
    }

    private Elements elementUtils;
    private Map<String, Schema> schemas;

    public Map<String, Schema> schemaMap() {
        return this.schemas;
    }

    public String generate(TypeElement element) throws Exception {

        String className = element.getSimpleName().toString();

        ApiModel apiModel = element.getAnnotation(ApiModel.class);
        String name = apiModel.name().trim();
        if (name.length() == 0) {
            name = className;
        }
        if (this.schemas.containsKey(name)) {
            return name;
        }

        ObjectSchema schema = new ObjectSchema();
        schema.name(name);
        schema.description(apiModel.description());


        List<? extends Element> enclosedElements = element.getEnclosedElements();

        for (Element enclosedElement : enclosedElements) {
            if (!enclosedElement.getKind().equals(ElementKind.FIELD)) {
                continue;
            }
            VariableElement variableElement = (VariableElement) enclosedElement;
            this.generateField(schema, variableElement);

        }

        this.schemas.put(name, schema);
        return name;
    }

    private void generateField(ObjectSchema schema, VariableElement variableElement) throws Exception {
        String propertyName = "";
        String propertyDescription = "";
        ApiModelProperty apiModelProperty = variableElement.getAnnotation(ApiModelProperty.class);
        if (apiModelProperty != null) {
            if (apiModelProperty.hidden()) {
                return;
            }
            propertyName = apiModelProperty.name().trim();
            propertyDescription = apiModelProperty.description().trim();
        }
        if (propertyName.isBlank()) {
            propertyName = variableElement.getSimpleName().toString();
        }

        Schema property;
        TypeName propertyTypeName = TypeName.get(variableElement.asType());
        if (propertyTypeName instanceof ParameterizedTypeName) {
            property = this.generateListField((ParameterizedTypeName) propertyTypeName, propertyName, propertyDescription);
        } else {
            property = this.generateLeaf(propertyTypeName, propertyName, propertyDescription);
        }
        schema.addProperties(variableElement.getSimpleName().toString(), property);

        if (this.checkRequired(variableElement)) {
            schema.addRequiredItem(variableElement.getSimpleName().toString());
        }
    }

    private boolean checkRequired(VariableElement variableElement) throws Exception {
        List<? extends AnnotationMirror> annotationMirrors = this.elementUtils.getAllAnnotationMirrors(variableElement);
        if (annotationMirrors == null || annotationMirrors.isEmpty()) {
            return false;
        }
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            Element annotationElement = annotationMirror.getAnnotationType().asElement();
            String pkg = this.elementUtils.getPackageOf(annotationElement).getSimpleName().toString();
            if (pkg.contains("jakarta.validation")) {
                return true;
            }
        }
        return false;
    }

    private ArraySchema generateListField(ParameterizedTypeName typeName, String name, String desc) throws Exception {
        if (!typeName.rawType.toString().equals("java.util.List")) {
            throw new Exception(String.format("%s:%s 类型不支持.", name, desc));
        }
        ArraySchema schema = new ArraySchema();
        if (name.length() > 0) {
            schema.name(name);
        }
        if (desc.length() > 0) {
            schema.description(desc);
        }
        TypeName argTypeName = typeName.typeArguments.get(0);
        if (argTypeName instanceof ParameterizedTypeName) {
            schema.items(this.generateListField((ParameterizedTypeName) argTypeName, "", ""));
            return schema;
        }
        schema.items(this.generateLeaf(argTypeName, "", ""));
        return schema;
    }

    private Schema generateLeaf(TypeName typeName, String name, String desc) throws Exception {
        Schema schema = null;
        if (typeName.equals(ClassName.get(String.class))) {
            schema = new StringSchema();
        } else if (typeName.equals(ClassName.get(Integer.class)) || typeName.equals(ClassName.INT)) {
            schema = new IntegerSchema().format("int32");
        } else if (typeName.equals(ClassName.get(Long.class)) || typeName.equals(ClassName.LONG)) {
            schema = new IntegerSchema().format("int64");
        } else if (typeName.equals(ClassName.get(Float.class)) || typeName.equals(ClassName.FLOAT)) {
            schema = new NumberSchema().format("float");
        } else if (typeName.equals(ClassName.get(Double.class)) || typeName.equals(ClassName.DOUBLE)) {
            schema = new NumberSchema().format("double");
        } else if (typeName.equals(ClassName.get(Boolean.class)) || typeName.equals(ClassName.BOOLEAN)) {
            schema = new BooleanSchema().format("double");
        } else if (typeName.equals(ClassName.get(Instant.class))) {
            schema = new StringSchema().format("date-time");
        } else if (typeName.equals(ClassName.get(LocalDate.class))) {
            schema = new StringSchema().format("date");
        } else if (typeName.equals(ClassName.get(LocalDateTime.class))) {
            schema = new StringSchema().format("date-time");
        } else if (typeName.equals(ClassName.get(OffsetDateTime.class))) {
            schema = new StringSchema().format("date-time");
        } else if (typeName.equals(ClassName.get(ZonedDateTime.class))) {
            schema = new StringSchema().format("date-time");
        } else if (typeName.equals(ClassName.get(Duration.class))) {
            schema = new StringSchema();
        } else {
            if (!(typeName instanceof ClassName)) {
                throw new Exception(String.format("%s:%s 类型不支持.", name, desc));
            }
            ClassName className = (ClassName) typeName;
            TypeElement typeElement = this.elementUtils.getTypeElement(className.packageName() + "." + className.simpleName());
            if (typeElement.getKind().equals(ElementKind.ENUM)) {
                // enum
                List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
                List<String> enums = new ArrayList<>();
                for (Element element : enclosedElements) {
                    if (element.getKind().equals(ElementKind.ENUM_CONSTANT)) {
                        VariableElement _enumElement = (VariableElement) element;
                        String enumValue;
                        if (_enumElement.getConstantValue() == null) {
                            enumValue = _enumElement.getSimpleName().toString();
                        } else {
                            enumValue = _enumElement.getConstantValue().toString();
                        }
                        enums.add("" + enumValue);
                    }
                }
                schema = new StringSchema()._enum(enums);
            }
            ApiModel apiModel = typeElement.getAnnotation(ApiModel.class);
            if (apiModel != null) {
                String modelName = apiModel.name().length() > 0 ? apiModel.name() : typeElement.getSimpleName().toString();
                if (this.schemas.containsKey(modelName)) {
                    schema = new ObjectSchema().$ref(String.format("#/components/schemas/%s", modelName));
                } else {
                    modelName = this.generate(typeElement);
                    schema = new ObjectSchema().$ref(String.format("#/components/schemas/%s", modelName));
                }
            }
        }
        if (schema == null) {
            throw new Exception(String.format("%s:%s 类型不支持.", name, desc));
        }

        if (name.length() > 0) {
            schema.name(name);
        }
        if (desc.length() > 0) {
            schema.description(desc);
        }
        return schema;
    }

}
