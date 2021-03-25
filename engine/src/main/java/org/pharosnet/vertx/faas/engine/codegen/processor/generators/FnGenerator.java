package org.pharosnet.vertx.faas.engine.codegen.processor.generators;


import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.vertx.codegen.annotations.ModuleGen;
import org.pharosnet.vertx.faas.engine.codegen.annotation.Fn;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Set;

public class FnGenerator {

    public FnGenerator(Messager messager, Elements elementUtils, TypeElement type, ModuleGen moduleGen) throws Exception {
        this.messager = messager;
        this.load(elementUtils, type, moduleGen);
    }

    private final Messager messager;

    private FnUnit fnUnit;
    private TypeMirror typeMirror;


    public void load(Elements elementUtils, TypeElement typeElement, ModuleGen moduleGen) throws Exception {
        String pkg = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
        String name = typeElement.getSimpleName().toString();
        this.typeMirror = typeElement.asType();
        this.fnUnit = new FnUnit();
        this.fnUnit.setModuleName(moduleGen.name());
        this.fnUnit.setFn(typeElement.getAnnotation(Fn.class));
        this.fnUnit.setClassName(name);
        this.fnUnit.setPackageName(pkg);
        this.scanFnClass(typeElement);
        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("加载到 %s.%s", pkg, name));
    }

    public FnUnit generate(Filer filer) throws Exception {
        // 生成fn的 service 和 service impl
        FnServiceGenerator fnServiceGenerator = new FnServiceGenerator(this.messager);
        fnServiceGenerator.generate(this.fnUnit, filer, this.typeMirror);
        // 生成fn的 proxy
        FnProxyGenerator fnProxyGenerator = new FnProxyGenerator(this.messager);
        fnProxyGenerator.generate(this.fnUnit, filer, this.typeMirror);
        // 生成fn的 router
        FnRouterGenerator fnRouterGenerator = new FnRouterGenerator(this.messager);
        fnRouterGenerator.generate(this.fnUnit, filer, this.typeMirror);
        return this.fnUnit;
    }

    private void scanFnClass(TypeElement typeElement) throws Exception {
        ExecutableElement methodElement = null;
        List<? extends Element> elements = typeElement.getEnclosedElements();
        for (Element element : elements) {
            if (!(element instanceof ExecutableElement)) {
                continue;
            }
            if (methodElement != null) {
                throw new Exception(String.format("%s.%s 类只能只有一个函数。", this.fnUnit.getPackageName(), this.fnUnit.getClassName()));
            }

            Set<Modifier> modifiers = element.getModifiers();
            if (modifiers.contains(Modifier.STATIC)) {
                continue;
            }

            methodElement = (ExecutableElement) element;
        }

        if (methodElement == null) {
            throw new Exception(String.format("%s.%s 类只能只有一个函数。", this.fnUnit.getPackageName(), this.fnUnit.getClassName()));
        }

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("获取函数 %s.%s:%s", this.fnUnit.getPackageName(), this.fnUnit.getClassName(), methodElement.getSimpleName()));

        this.fnUnit.setMethodName(methodElement.getSimpleName().toString());

        TypeMirror returnTypeMirror = methodElement.getReturnType();
        TypeName returnType = TypeName.get(returnTypeMirror);

        if (!returnType.toString().startsWith("io.vertx.core.Future")) {
            throw new Exception(String.format("%s.%s 函数的返回值不是io.vertx.core.Future。", this.fnUnit.getPackageName(), this.fnUnit.getClassName()));
        }

        ParameterizedTypeName returnParameterizedType = (ParameterizedTypeName) returnType;
        this.fnUnit.setReturnElementClass(returnParameterizedType.typeArguments.get(0));
        this.fnUnit.setReturnClass(returnParameterizedType);

        List<? extends VariableElement> parameters = methodElement.getParameters();
        if (parameters == null || parameters.size() == 0) {
            throw new Exception(String.format("%s.%s 函数的参数为空。", this.fnUnit.getPackageName(), this.fnUnit.getClassName()));
        }

        for (int i = 0; i < parameters.size(); i++) {
            VariableElement parameter = parameters.get(i);
            if (i == 0) {
                if (!TypeName.get(parameter.asType()).toString().equals("org.pharosnet.vertx.faas.engine.context.FnContext")) {
                    throw new Exception(String.format("%s.%s 函数的第一个参数不是org.pharosnet.vertx.faas.engine.context.FnContext。", this.fnUnit.getPackageName(), this.fnUnit.getClassName()));
                }
            }
            this.fnUnit.getParameters().add(parameter);
        }

    }

}
