package org.pharosnet.vertx.faas.codegen.processor.generators;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import org.pharosnet.vertx.faas.codegen.annotation.Fn;
import org.pharosnet.vertx.faas.context.Context;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;

public class FnGenerator {

    public FnGenerator(Messager messager, Elements elementUtils, TypeElement type) throws Exception {
        this.messager = messager;
        this.load(elementUtils, type);
    }

    private final Messager messager;

    private FnUnit fnUnit;
    private TypeMirror typeMirror;

    public void load(Elements elementUtils, TypeElement typeElement) throws Exception {
        String pkg = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
        String name = typeElement.getSimpleName().toString();
        this.typeMirror = typeElement.asType();
        this.fnUnit = new FnUnit();
        this.fnUnit.setFn(typeElement.getAnnotation(Fn.class));
        this.fnUnit.setClassName(name);
        this.fnUnit.setPackageName(pkg);
        this.scanFnClass(pkg, name);
        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("加载到 %s.%s", pkg, name));
    }

    public FnUnit generate(Filer filer) throws Exception {
        // 生成fn的 service 和 service impl
        FnServiceGenerator fnServiceGenerator = new FnServiceGenerator(this.messager);
        fnServiceGenerator.generate(this.fnUnit, filer, this.typeMirror);
        // 生成fn的 proxy

        // 生成fn的 router
        return this.fnUnit;
    }

    private void scanFnClass(String pkg, String name) throws Exception {
        Class<?> fnClass = Class.forName(String.format("%s.%s", pkg, name));
        Method[] methods = fnClass.getMethods();
        if (methods.length != 1) {
            throw new Exception(String.format("%s.%s 类只能只有一个函数。", pkg, name));
        }

        Method method = methods[0];

        Class<?> returnClass = method.getReturnType();
        if (!(returnClass.getPackageName().equals("io.vertx.core") && returnClass.getSimpleName().equals("Future"))) {
            throw new Exception(String.format("%s.%s 函数的返回值不是io.vertx.core.Future。", pkg, name));
        }

        ParameterizedType returnParameterizedType = (ParameterizedType) returnClass.getGenericSuperclass();
        Class<?> returnElementClass = (Class<?>) returnParameterizedType.getActualTypeArguments()[0];
        this.fnUnit.setReturnElementClass(returnElementClass);
        this.fnUnit.setReturnClass(ParameterizedTypeName.get(
                ClassName.get(returnClass),
                ClassName.get(returnElementClass)));

        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            throw new Exception(String.format("%s.%s 函数的参数为空。", pkg, name));
        }

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (i == 0) {
                if (parameter.getType() != Context.class) {
                    throw new Exception(String.format("%s.%s 函数的第一个参数不是org.pharosnet.vertx.faas.context.Context。", pkg, name));
                }
                this.fnUnit.getParameters().add(parameter);
            }
        }

    }

}
