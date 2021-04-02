package org.pharosnet.vertx.faas.codegen.generators;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.format.CamelCase;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

public class DataObjectPlusGenerator {

    public DataObjectPlusGenerator(Messager messager, Elements elementUtils, Types typeUtils, Filer filer) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
        this.filer = filer;
    }

    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    public void generate(Element element) throws Exception {
        DataObject dataObject = element.getAnnotation(DataObject.class);
        if (!dataObject.generateConverter()) {
            return;
        }
        if (!(element instanceof TypeElement)) {
            throw new Exception("不是类。");
        }
        TypeElement typeElement = (TypeElement) element;
        List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
        if (enclosedElements == null || enclosedElements.isEmpty()) {
            throw new Exception("缺失以属性。");
        }
        List<VariableElement> fieldElements = new ArrayList<>();
        boolean hasConstructor = false;
        boolean hasToJsonMethod = false;
        for (Element enclosedElement : enclosedElements) {
            if (enclosedElement.getKind().equals(ElementKind.FIELD)) {
                if (enclosedElement.getAnnotation(JsonIgnore.class) == null) {
                    fieldElements.add((VariableElement) enclosedElement);
                }
                continue;
            }
            if (enclosedElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
                ExecutableElement constructorElement = (ExecutableElement) enclosedElement;
                List<? extends VariableElement> paramElements = constructorElement.getParameters();
                if (paramElements == null || paramElements.isEmpty()) {
                    continue;
                }
                if (paramElements.size() != 1) {
                    continue;
                }
                TypeName typeName = TypeName.get(paramElements.get(0).asType());
                if (typeName.toString().equals("io.vertx.core.json.JsonObject")) {
                    if (constructorElement.getModifiers().contains(Modifier.PUBLIC)) {
                        hasConstructor = true;
                    }
                    continue;
                }
            }
            if (enclosedElement instanceof ExecutableElement) {
                ExecutableElement methodElement = (ExecutableElement) enclosedElement;
                if (methodElement.getSimpleName().toString().equals("toJson")) {
                    List<? extends VariableElement> paramElements = methodElement.getParameters();
                    if (paramElements == null || paramElements.isEmpty()) {
                        TypeName typeName = TypeName.get(methodElement.getReturnType());
                        if (typeName.toString().equals("io.vertx.core.json.JsonObject")) {
                            if (methodElement.getModifiers().contains(Modifier.PUBLIC) && methodElement.getModifiers().size() == 1) {
                                hasToJsonMethod = true;
                            }
                        }
                    }
                }
            }
        }

        if (!hasConstructor || !hasToJsonMethod) {
            throw new Exception("缺失以JsonObject为参数的PUBLIC构造函数或public JsonObject toJson()函数。");
        }
        if (fieldElements.isEmpty()) {
            throw new Exception("缺失以属性。");
        }
        this.generateMapper(typeElement, fieldElements);
    }

    private void generateMapper(TypeElement typeElement, List<VariableElement> fieldElements) throws Exception {
        String pkg = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
        String mapperClassName = String.format("%sJsonMapper", CamelCase.INSTANCE.format(List.of(typeElement.getSimpleName().toString())));

        // class
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(mapperClassName)
                .addModifiers(Modifier.PUBLIC);

        // from json
        MethodSpec.Builder fromMethod = new DataObjectPlusFromJsonGenerator(this.elementUtils, this.typeUtils).generate(typeElement, fieldElements);
        // to json
        MethodSpec.Builder toMethod = new DataObjectPlusToJsonGenerator(this.elementUtils, this.typeUtils).generate(typeElement, fieldElements);

        typeBuilder.addMethod(fromMethod.build());
        typeBuilder.addMethod(toMethod.build());

        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder.build())
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
                .indent("\t")
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, mapperClassName));
    }



}
