package org.pharosnet.vertx.faas.codegen.processor.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.vertx.codegen.format.CamelCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.pharosnet.vertx.faas.codegen.annotation.Fn;
import org.pharosnet.vertx.faas.codegen.annotation.PathParam;
import org.pharosnet.vertx.faas.codegen.annotation.QueryParam;
import org.pharosnet.vertx.faas.codegen.annotation.RequestBody;
import org.pharosnet.vertx.faas.codegen.annotation.oas.ApiModel;
import org.pharosnet.vertx.faas.codegen.annotation.oas.ApiModelProperty;
import org.pharosnet.vertx.faas.component.http.HttpMethod;

import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.lang.annotation.ElementType;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class OASPathsGenerator {

    public OASPathsGenerator(Elements elementUtils) {
        this.elementUtils = elementUtils;
        this.schemas = new HashMap<>();
    }

    private Elements elementUtils;

    private Map<String, Schema> schemas;

    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    protected Paths generate(Map<String, List<Element>> moduleFnMap) throws Exception {
        Paths paths = new Paths();
        Set<String> moduleNames = moduleFnMap.keySet();
        for (String moduleName : moduleNames) {
            List<Element> elements = moduleFnMap.get(moduleName);
            for (Element element : elements) {
                this.buildPathItem(paths, moduleName, element);
            }
        }
        return paths;
    }

    private void buildPathItem(Paths paths, String moduleName, Element element) throws Exception {
        Fn fn = element.getAnnotation(Fn.class);
        String path = fn.path().trim();
        if (path.length() == 0) {
            path = String.format("/%s/%s", moduleName, element.getSimpleName());
        }
        PathItem pathItem = paths.get(path);
        if (pathItem == null) {
            pathItem = new PathItem();
            paths.addPathItem(path, pathItem);
        }

        Operation operation = this.buildOperation(moduleName, fn, element);

        if (fn.method().equals(HttpMethod.OPTIONS)) {
            pathItem.options(operation);
        } else if (fn.method().equals(HttpMethod.GET)) {
            pathItem.get(operation);
        } else if (fn.method().equals(HttpMethod.HEAD)) {
            pathItem.head(operation);
        } else if (fn.method().equals(HttpMethod.POST)) {
            pathItem.post(operation);
        } else if (fn.method().equals(HttpMethod.PUT)) {
            pathItem.put(operation);
        } else if (fn.method().equals(HttpMethod.DELETE)) {
            pathItem.delete(operation);
        } else if (fn.method().equals(HttpMethod.TRACE)) {
            pathItem.trace(operation);
        } else if (fn.method().equals(HttpMethod.PATCH)) {
            pathItem.patch(operation);
        } else {
            throw new Exception("未知HTTP METHOD");
        }
    }

    private Operation buildOperation(String moduleName, Fn fn, Element element) throws Exception {
        Operation operation = new Operation();
        operation.operationId(CamelCase.INSTANCE.format(List.of(moduleName, element.getSimpleName().toString())));
        operation.description(fn.description().trim());
        operation.summary(fn.summary().trim());
        // tags
        for (String tag : fn.tags()) {
            if (tag.trim().length() == 0) {
                continue;
            }
            operation.addTagsItem(tag.trim());
        }

        ExecutableElement methodElement = null;
        List<? extends Element> enclosedElements = element.getEnclosedElements();
        for (Element enclosedElement : enclosedElements) {
            if (!(enclosedElement instanceof ExecutableElement)) {
                continue;
            }
            if (methodElement != null) {
                throw new Exception(String.format("%s.%s 类只能只有一个函数。", moduleName, element.getSimpleName()));
            }

            Set<Modifier> modifiers = enclosedElement.getModifiers();
            if (modifiers.contains(Modifier.STATIC)) {
                continue;
            }
            methodElement = (ExecutableElement) enclosedElement;

        }
        if (methodElement == null) {
            throw new Exception(String.format("%s.%s 类只能只有一个函数。", moduleName, element.getSimpleName()));
        }
        // parameters or requestBody
        List<? extends VariableElement> fnParams = methodElement.getParameters();
        if (fnParams != null && !fnParams.isEmpty()) {
            for (int i = 1; i < fnParams.size(); i++) {
                VariableElement fnParam = fnParams.get(i);
                // path param
                PathParam pathParam = fnParam.getAnnotation(PathParam.class);
                if (pathParam != null) {
                    Parameter parameter = new Parameter();
                    parameter.in("path");
                    parameter.name(pathParam.value());
                    parameter.description(pathParam.description());
                    parameter.required(true);
                    operation.addParametersItem(parameter);
                }
                // query param
                QueryParam queryParam = fnParam.getAnnotation(QueryParam.class);
                if (queryParam != null) {
                    Parameter parameter = new Parameter();
                    parameter.in("query");
                    parameter.name(queryParam.value());
                    parameter.description(queryParam.description());

                    // valid required
                    NotNull notNull = fnParam.getAnnotation(NotNull.class);
                    NotBlank notBlank = fnParam.getAnnotation(NotBlank.class);
                    if (notNull != null || notBlank != null) {
                        parameter.required(true);
                    }
                    // valid pattern
                    Pattern pattern = fnParam.getAnnotation(Pattern.class);
                    if (pattern != null) {
                        parameter.addExtension("pattern", pattern.regexp());
                    }

                    operation.addParametersItem(parameter);
                }

                // request body
                RequestBody requestBody = fnParam.getAnnotation(RequestBody.class);
                if (requestBody != null) {
                    TypeElement requestBodyType = (TypeElement) fnParam.asType();

                    String requestBodySchemaName = this.buildObjectSchema(requestBodyType);

                    String mediaTypeName = fn.consumes().trim();
                    if (mediaTypeName.length() == 0) {
                        mediaTypeName = "application/json";
                    }

                    io.swagger.v3.oas.models.parameters.RequestBody body = new io.swagger.v3.oas.models.parameters.RequestBody();
                    body.required(fnParam.getAnnotation(Valid.class) != null);
                    body.description(requestBody.description());
                    body.content(new Content()
                            .addMediaType(mediaTypeName,
                                    new MediaType()
                                            .schema(new Schema()
                                                    .$ref(String.format("#/components/schemas/%s", requestBodySchemaName))))
                    );

                    operation.requestBody(body);
                }
            }
        }


        // responses
        ApiResponses apiResponses = new ApiResponses();
        ApiResponse succeedApiResponse = new ApiResponse();
        ParameterizedTypeName returnTypeName = (ParameterizedTypeName) TypeName.get(methodElement.getReturnType());
        ClassName returnTypeClassName = (ClassName) returnTypeName.typeArguments.get(0);
        if (!returnTypeClassName.equals(ClassName.VOID)) {
            TypeElement returnType = this.elementUtils.getTypeElement(returnTypeClassName.packageName() + "." + returnTypeClassName.simpleName());
            boolean array = false;
            if (TypeName.get(returnType.asType()) instanceof ParameterizedTypeName) {
                array = true;
                returnTypeName = (ParameterizedTypeName) TypeName.get(returnType.asType());
                returnTypeClassName = (ClassName) returnTypeName.typeArguments.get(0);
                returnType = this.elementUtils.getTypeElement(returnTypeClassName.packageName() + "." + returnTypeClassName.simpleName());
            }
            //
            returnTypeClassName = ClassName.get(returnType);
            Schema responseSchema = null;
            if (returnTypeClassName.equals(ClassName.get(String.class))) {
                responseSchema = new StringSchema();
            } else if (returnTypeClassName.equals(ClassName.get(Integer.class)) || returnTypeClassName.equals(ClassName.INT)) {
                responseSchema = new IntegerSchema().format("int32");

            } else if (returnTypeClassName.equals(ClassName.get(Long.class)) || returnTypeClassName.equals(ClassName.LONG)) {
                responseSchema = new IntegerSchema().format("int64");
            } else if (returnTypeClassName.equals(ClassName.get(Float.class)) || returnTypeClassName.equals(ClassName.FLOAT)) {
                responseSchema = new NumberSchema().format("float");
            } else if (returnTypeClassName.equals(ClassName.get(Double.class)) || returnTypeClassName.equals(ClassName.DOUBLE)) {
                responseSchema = new NumberSchema().format("double");
            } else if (returnTypeClassName.equals(ClassName.get(Boolean.class)) || returnTypeClassName.equals(ClassName.BOOLEAN)) {
                responseSchema = new BooleanSchema().format("double");
            } else if (returnTypeClassName.equals(ClassName.get(Instant.class))) {
                responseSchema = new StringSchema().format("date-time");
            } else if (returnTypeClassName.equals(ClassName.get(LocalDate.class))) {
                responseSchema = new StringSchema().format("date");
            } else if (returnTypeClassName.equals(ClassName.get(LocalDateTime.class))) {
                responseSchema = new StringSchema().format("date-time");
            } else if (returnTypeClassName.equals(ClassName.get(OffsetDateTime.class))) {
                responseSchema = new StringSchema().format("date-time");
            } else if (returnTypeClassName.equals(ClassName.get(ZonedDateTime.class))) {
                responseSchema = new StringSchema().format("date-time");
            } else {
                String returnTypeSchemaName = this.buildObjectSchema(returnType);
                responseSchema = new ObjectSchema().$ref(String.format("#/components/schemas/%s", returnTypeSchemaName));
            }

            if (array) {
                responseSchema = new ArraySchema().items(responseSchema);
            }

            succeedApiResponse.content(new Content()
                    .addMediaType(fn.produces(), new MediaType().schema(responseSchema)));
        }
        apiResponses.addApiResponse(String.format("%d", fn.succeedStatus()), succeedApiResponse);

        operation.responses(apiResponses);

        // deprecated
        if (element.getAnnotation(Deprecated.class) != null) {
            operation.deprecated(true);
        }

        // authentication
        if (fn.authentication()) {
            operation.addSecurityItem(new SecurityRequirement().addList("authentication", "true"));
        }

        return operation;
    }

    // return schemas.key = name
    private String buildObjectSchema(TypeElement element) throws Exception {
        ApiModel apiModel = element.getAnnotation(ApiModel.class);
        String name = apiModel.name().trim();
        if (name.length() == 0) {
            name = element.getSimpleName().toString();
        }
        Schema schema = this.getSchema(element);
        if (schema != null) {
            return name;
        }

        schema = new ObjectSchema();

        schema.description(apiModel.description());

        List<String> requiredProperties = new ArrayList<>();

        List<? extends Element> requestBodyTypeEnclosedElements = element.getEnclosedElements();
        for (Element requestBodyTypeEnclosedElement : requestBodyTypeEnclosedElements) {
            ApiModelProperty apiModelProperty = requestBodyTypeEnclosedElement.getAnnotation(ApiModelProperty.class);
            if (apiModelProperty != null) {
                if (apiModelProperty.hidden()) {
                    continue;
                }
                String propertyName = apiModelProperty.name().trim();
                if (propertyName.length() == 0) {
                    propertyName = requestBodyTypeEnclosedElement.getSimpleName().toString();
                }
                Schema property = null;
//                Element propertyType =  requestBodyTypeEnclosedElement;
                VariableElement propertyType = (VariableElement) requestBodyTypeEnclosedElement;
                TypeName propertyTypeClassName = TypeName.get(propertyType.asType());
                //  list
                TypeName propertyTypeName = TypeName.get(propertyType.asType());
                if (propertyTypeName instanceof ParameterizedTypeName) {
                    ParameterizedTypeName propertyTypeParameterizedTypeName = (ParameterizedTypeName) propertyTypeName;
                    if (propertyTypeParameterizedTypeName.rawType.equals(ClassName.get(List.class)) || propertyTypeParameterizedTypeName.rawType.equals(ClassName.get(Set.class))) {
                        ClassName typeArgumentTypeName = (ClassName) propertyTypeParameterizedTypeName.typeArguments.get(0);

                        if (propertyTypeClassName.equals(ClassName.get(String.class))) {
                            property = new ArraySchema()
                                    .items(new StringSchema());
                        } else if (propertyTypeClassName.equals(ClassName.get(Integer.class)) || propertyTypeClassName.equals(ClassName.INT)) {
                            property = new ArraySchema()
                                    .items(new IntegerSchema().format("int32"));

                        } else if (propertyTypeClassName.equals(ClassName.get(Long.class)) || propertyTypeClassName.equals(ClassName.LONG)) {
                            property = new ArraySchema()
                                    .items(new IntegerSchema().format("int64"));
                        } else if (propertyTypeClassName.equals(ClassName.get(Float.class)) || propertyTypeClassName.equals(ClassName.FLOAT)) {
                            property = new ArraySchema()
                                    .items(new NumberSchema().format("float"));
                        } else if (propertyTypeClassName.equals(ClassName.get(Double.class)) || propertyTypeClassName.equals(ClassName.DOUBLE)) {
                            property = new ArraySchema()
                                    .items(new NumberSchema().format("double"));
                        } else if (propertyTypeClassName.equals(ClassName.get(Boolean.class)) || propertyTypeClassName.equals(ClassName.BOOLEAN)) {
                            property = new ArraySchema()
                                    .items(new BooleanSchema().format("double"));
                        } else if (propertyTypeClassName.equals(ClassName.get(Instant.class))) {
                            property = new ArraySchema()
                                    .items(new StringSchema().format("date-time"));
                        } else if (propertyTypeClassName.equals(ClassName.get(LocalDate.class))) {
                            property = new ArraySchema()
                                    .items(new StringSchema().format("date"));
                        } else if (propertyTypeClassName.equals(ClassName.get(LocalDateTime.class))) {
                            property = new ArraySchema()
                                    .items(new StringSchema().format("date-time"));
                        } else if (propertyTypeClassName.equals(ClassName.get(OffsetDateTime.class))) {
                            property = new ArraySchema()
                                    .items(new StringSchema().format("date-time"));
                        } else if (propertyTypeClassName.equals(ClassName.get(ZonedDateTime.class))) {
                            property = new ArraySchema()
                                    .items(new StringSchema().format("date-time"));
                        } else {
                            TypeElement typeArgumentType = this.elementUtils.getTypeElement(typeArgumentTypeName.packageName() + "." + typeArgumentTypeName.simpleName());

                            ApiModel propertyTypeApiModel = typeArgumentType.getAnnotation(ApiModel.class);
                            if (propertyTypeApiModel == null) {
                                throw new Exception("无法解析 " + element.getSimpleName().toString() + "." + propertyName + " 需要@ApiModel");
                            }

                            Schema propertyTypeSchema = this.getSchema((TypeElement) propertyType);
                            String propertyTypeSchemaName;
                            if (propertyTypeSchema == null) {
                                propertyTypeSchemaName = this.buildObjectSchema((TypeElement) propertyType);
                            } else {
                                propertyTypeSchemaName = propertyTypeApiModel.name().trim();
                                if (propertyTypeSchemaName.length() == 0) {
                                    propertyTypeSchemaName = propertyType.getSimpleName().toString();
                                }

                            }

                            property = new ArraySchema()
                                    .items(new ObjectSchema().$ref(String.format("#/components/schemas/%s", propertyTypeSchemaName)));

                        }

                    }
                } else {
                    if (propertyTypeClassName.equals(ClassName.get(String.class))) {
                        property = new StringSchema();
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        NotBlank notBlank = requestBodyTypeEnclosedElement.getAnnotation(NotBlank.class);
                        if (notNull != null || notBlank != null) {
                            requiredProperties.add(propertyName);
                        }
                        Pattern pattern = requestBodyTypeEnclosedElement.getAnnotation(Pattern.class);
                        if (pattern != null) {
                            property.pattern(pattern.regexp());
                        }
                    } else if (propertyTypeClassName.equals(ClassName.get(Integer.class)) || propertyTypeClassName.equals(ClassName.INT)) {
                        property = new IntegerSchema();
                        property.format("int32");
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        if (notNull != null) {
                            requiredProperties.add(propertyName);
                        }
                        Max max = requestBodyTypeEnclosedElement.getAnnotation(Max.class);
                        if (max != null) {
                            property.maximum(new BigDecimal(max.value()));
                        }
                        Min min = requestBodyTypeEnclosedElement.getAnnotation(Min.class);
                        if (min != null) {
                            property.minimum(new BigDecimal(min.value()));
                        }
                    } else if (propertyTypeClassName.equals(ClassName.get(Long.class)) || propertyTypeClassName.equals(ClassName.LONG)) {
                        property = new IntegerSchema();
                        property.format("int64");
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        if (notNull != null) {
                            requiredProperties.add(propertyName);
                        }
                        Max max = requestBodyTypeEnclosedElement.getAnnotation(Max.class);
                        if (max != null) {
                            property.maximum(new BigDecimal(max.value()));
                        }
                        Min min = requestBodyTypeEnclosedElement.getAnnotation(Min.class);
                        if (min != null) {
                            property.minimum(new BigDecimal(min.value()));
                        }
                    } else if (propertyTypeClassName.equals(ClassName.get(Float.class)) || propertyTypeClassName.equals(ClassName.FLOAT)) {
                        property = new NumberSchema();
                        property.format("float");
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        if (notNull != null) {
                            requiredProperties.add(propertyName);
                        }
                        Max max = requestBodyTypeEnclosedElement.getAnnotation(Max.class);
                        if (max != null) {
                            property.maximum(new BigDecimal(max.value()));
                        }
                        Min min = requestBodyTypeEnclosedElement.getAnnotation(Min.class);
                        if (min != null) {
                            property.minimum(new BigDecimal(min.value()));
                        }
                        DecimalMax decimalMax = requestBodyTypeEnclosedElement.getAnnotation(DecimalMax.class);
                        if (decimalMax != null) {
                            property.maximum(new BigDecimal(decimalMax.value()));
                        }
                        DecimalMin decimalMin = requestBodyTypeEnclosedElement.getAnnotation(DecimalMin.class);
                        if (decimalMin != null) {
                            property.maximum(new BigDecimal(decimalMin.value()));
                        }
                    } else if (propertyTypeClassName.equals(ClassName.get(Double.class)) || propertyTypeClassName.equals(ClassName.DOUBLE)) {
                        property = new NumberSchema();
                        property.format("double");
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        if (notNull != null) {
                            requiredProperties.add(propertyName);
                        }
                        Max max = requestBodyTypeEnclosedElement.getAnnotation(Max.class);
                        if (max != null) {
                            property.maximum(new BigDecimal(max.value()));
                        }
                        Min min = requestBodyTypeEnclosedElement.getAnnotation(Min.class);
                        if (min != null) {
                            property.minimum(new BigDecimal(min.value()));
                        }
                        DecimalMax decimalMax = requestBodyTypeEnclosedElement.getAnnotation(DecimalMax.class);
                        if (decimalMax != null) {
                            property.maximum(new BigDecimal(decimalMax.value()));
                        }
                        DecimalMin decimalMin = requestBodyTypeEnclosedElement.getAnnotation(DecimalMin.class);
                        if (decimalMin != null) {
                            property.maximum(new BigDecimal(decimalMin.value()));
                        }
                    } else if (propertyTypeClassName.equals(ClassName.get(Boolean.class)) || propertyTypeClassName.equals(ClassName.BOOLEAN)) {
                        property = new BooleanSchema();
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        if (notNull != null) {
                            requiredProperties.add(propertyName);
                        }
                    } else if (propertyTypeClassName.equals(ClassName.get(Instant.class))) {
                        property = new StringSchema();
                        property.format("date-time");
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        if (notNull != null) {
                            requiredProperties.add(propertyName);
                        }
                    } else if (propertyTypeClassName.equals(ClassName.get(LocalDate.class))) {
                        property = new StringSchema();
                        property.format("date");
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        if (notNull != null) {
                            requiredProperties.add(propertyName);
                        }
                    } else if (propertyTypeClassName.equals(ClassName.get(LocalDateTime.class))) {
                        property = new StringSchema();
                        property.format("date-time");
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        if (notNull != null) {
                            requiredProperties.add(propertyName);
                        }
                    } else if (propertyTypeClassName.equals(ClassName.get(OffsetDateTime.class))) {
                        property = new StringSchema();
                        property.format("date-time");
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        if (notNull != null) {
                            requiredProperties.add(propertyName);
                        }
                    } else if (propertyTypeClassName.equals(ClassName.get(ZonedDateTime.class))) {
                        property = new StringSchema();
                        property.format("date-time");
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        if (notNull != null) {
                            requiredProperties.add(propertyName);
                        }
                    } else if (propertyType.getKind().equals(ElementKind.ENUM)) {
                        // enum
                        property = new StringSchema();
                        // valid
                        NotNull notNull = requestBodyTypeEnclosedElement.getAnnotation(NotNull.class);
                        if (notNull != null) {
                            requiredProperties.add(propertyName);
                        }
                        List<? extends Element> enumTypeElements = propertyType.getEnclosedElements();
                        if (enumTypeElements != null) {
                            for (Element enumTypeElement : enumTypeElements) {
                                if (enumTypeElement.getKind().equals(ElementKind.ENUM_CONSTANT)) {
                                    property.addEnumItemObject(enumTypeElement.getSimpleName().toString());
                                }
                            }
                        }
                    } else {
                        ClassName propertyTypeClassName0 = (ClassName)TypeName.get(propertyType.asType());
                        TypeElement propertyTypeClass = this.elementUtils.getTypeElement(propertyTypeClassName0.packageName() + "." + propertyTypeClassName0.simpleName());
                        // object
                        ApiModel propertyTypeApiModel = propertyTypeClass.getAnnotation(ApiModel.class);
                        if (propertyTypeApiModel == null) {
                            throw new Exception("无法解析 " + element.getSimpleName().toString() + "." + propertyName + " 需要@ApiModel");
                        }

                        Schema propertyTypeSchema = this.getSchema(propertyTypeClass);
                        String propertyTypeSchemaName;
                        if (propertyTypeSchema == null) {
                            propertyTypeSchemaName = this.buildObjectSchema(propertyTypeClass);
                        } else {
                            propertyTypeSchemaName = propertyTypeApiModel.name().trim();
                            if (propertyTypeSchemaName.length() == 0) {
                                propertyTypeSchemaName = propertyTypeClass.getSimpleName().toString();
                            }

                        }
                        property = new ObjectSchema()
                                .$ref(String.format("#/components/schemas/%s", propertyTypeSchemaName));
                    }
                }


                if (property == null) {
                    throw new Exception("无法解析 " + element.getSimpleName().toString() + "." + propertyName);
                }
                property.description(apiModelProperty.description());
                schema.addProperties(propertyName, property);
            }
        }
        if (!requiredProperties.isEmpty()) {
            schema.required(requiredProperties);
        }

        this.schemas.put(name, schema);
        return name;
    }

    private Schema getSchema(TypeElement element) {
        ApiModel apiModel = element.getAnnotation(ApiModel.class);
        String name = apiModel.name().trim();
        if (name.length() == 0) {
            name = element.getSimpleName().toString();
        }
        return this.schemas.get(name);
    }

}
