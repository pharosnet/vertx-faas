package org.pharosnet.vertx.faas.codegen.generators;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.pharosnet.vertx.faas.codegen.annotation.Fn;
import org.pharosnet.vertx.faas.codegen.annotation.FnModule;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Set;

public class FnImplGenerator {

    public FnImplGenerator(Messager messager, Elements elementUtils, FnImpl fnImpl) throws Exception {
        this.elementUtils = elementUtils;
        this.messager = messager;
        this.fnImpl = fnImpl;
        this.load(elementUtils, fnImpl);
    }

    private final Elements elementUtils;
    private final Messager messager;
    private TypeMirror typeMirror;
    private final FnImpl fnImpl;


    public void load(Elements elementUtils, FnImpl fnImpl) throws Exception {
        String pkg = elementUtils.getPackageOf(fnImpl.getTypeElement()).getQualifiedName().toString();
        String name = fnImpl.getTypeElement().getSimpleName().toString();
        fnImpl.setPkg(pkg);
        fnImpl.setClassName(name);
        this.typeMirror = fnImpl.getTypeElement().asType();
        this.scanFnClass(fnImpl.getInterfaceTypeElement());
        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("加载到 %s.%s", pkg, name));
    }

    public FnImpl generate(Filer filer) throws Exception {
        // 生成fn的 service impl
        FnServiceImplGenerator fnServiceGenerator = new FnServiceImplGenerator(this.messager);
        fnServiceGenerator.generate(this.fnImpl, filer, this.typeMirror);
        // 生成fn的 router
        FnRouterGenerator fnRouterGenerator = new FnRouterGenerator(this.elementUtils, this.messager);
        fnRouterGenerator.generate(this.fnImpl, filer, this.typeMirror);
        return this.fnImpl;
    }

    private void scanFnClass(TypeElement typeElement) throws Exception {
        FnUnit fnUnit = new FnUnit();
        ExecutableElement methodElement = null;
        List<? extends Element> elements = typeElement.getEnclosedElements();
        for (Element element : elements) {
            if (!(element instanceof ExecutableElement)) {
                continue;
            }
            if (methodElement != null) {
                throw new Exception(String.format("%s 类只能只有一个函数。", typeElement.getSimpleName().toString()));
            }

            Set<Modifier> modifiers = element.getModifiers();
            if (modifiers.contains(Modifier.STATIC)) {
                continue;
            }

            methodElement = (ExecutableElement) element;
        }

        if (methodElement == null) {
            throw new Exception(String.format("%s 类只能只有一个函数。", typeElement.getSimpleName().toString()));
        }

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("获取函数 %s:%s", typeElement.getSimpleName().toString(), methodElement.getSimpleName()));

        fnUnit.setFn(typeElement.getAnnotation(Fn.class));
        fnUnit.setClassName(typeElement.getSimpleName().toString());
        fnUnit.setPackageName(elementUtils.getPackageOf(typeElement).getQualifiedName().toString());
        fnUnit.setImplTypeElement(fnImpl.getTypeElement());

        fnUnit.setMethodName(methodElement.getSimpleName().toString());

        TypeMirror returnTypeMirror = methodElement.getReturnType();
        TypeName returnType = TypeName.get(returnTypeMirror);

        if (!returnType.toString().startsWith("io.vertx.core.Future")) {
            throw new Exception(String.format("%s 函数的返回值不是io.vertx.core.Future。", typeElement.getSimpleName().toString()));
        }

        ParameterizedTypeName returnParameterizedType = (ParameterizedTypeName) returnType;
        fnUnit.setReturnElementClass(returnParameterizedType.typeArguments.get(0));
        fnUnit.setReturnClass(returnParameterizedType);

        List<? extends VariableElement> parameters = methodElement.getParameters();
        if (parameters == null || parameters.size() == 0) {
            throw new Exception(String.format("%s 函数的参数为空。", typeElement.getSimpleName().toString()));
        }

        for (int i = 0; i < parameters.size(); i++) {
            VariableElement parameter = parameters.get(i);
            if (i == 0) {
                if (!TypeName.get(parameter.asType()).toString().equals("org.pharosnet.vertx.faas.http.context.FnContext")) {
                    throw new Exception(String.format("%s 函数的第一个参数不是org.pharosnet.vertx.faas.http.context.FnContext。", typeElement.getSimpleName().toString()));
                }
            }
            fnUnit.getParameters().add(parameter);
        }
        this.fnImpl.setFnUnit(fnUnit);
    }

}
