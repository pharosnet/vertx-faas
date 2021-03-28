package org.pharosnet.vertx.faas.engine.codegen.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import io.vertx.codegen.annotations.ModuleGen;
import org.pharosnet.vertx.faas.engine.codegen.annotation.EnableOAS;
import org.pharosnet.vertx.faas.engine.codegen.annotation.Fn;
import org.pharosnet.vertx.faas.engine.codegen.processor.generators.FnImpl;
import org.pharosnet.vertx.faas.engine.codegen.processor.generators.ModuleFnGenerator;
import org.pharosnet.vertx.faas.engine.codegen.processor.generators.ModuleImplGenerator;
import org.pharosnet.vertx.faas.engine.codegen.processor.generators.OASGenerator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.*;

@SupportedAnnotationTypes({
        "io.vertx.codegen.annotations.ModuleGen",
        "org.pharosnet.vertx.faas.codegen.annotation.EnableOAS",
})
@SupportedOptions({"codegen.output"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class FaaSCodeGenProcessor extends AbstractProcessor {

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<String, List<Element>> moduleFnMap = this.generateModuleFn(roundEnv);
        this.generateOAS(roundEnv, moduleFnMap);
        return true;
    }

    private void generateOAS(RoundEnvironment roundEnv, Map<String, List<Element>> moduleFnMap) {
        Set<? extends Element> oasElements = roundEnv.getElementsAnnotatedWith(EnableOAS.class);
        if (oasElements == null || oasElements.isEmpty()) {
            return;
        }
        Element oasElement = oasElements.iterator().next();
        EnableOAS enableOAS = oasElement.getAnnotation(EnableOAS.class);
        try {
            new OASGenerator(this.messager, this.elementUtils, this.filer).generate(moduleFnMap, enableOAS);
        } catch (Exception exception) {
            messager.printMessage(Diagnostic.Kind.ERROR, "生成 OpenAPI 失败。");
            messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
            throw new RuntimeException(exception);
        }
    }

    private Map<String, List<Element>> generateModuleFn(RoundEnvironment roundEnv) {
        Map<String, List<Element>> moduleFnMap = new HashMap<>();

        Set<? extends Element> moduleElements = roundEnv.getElementsAnnotatedWith(ModuleGen.class);
        if (moduleElements == null || moduleElements.isEmpty()) {
            return moduleFnMap;
        }
        for (Element moduleElement : moduleElements) {
            String packageName = elementUtils.getPackageOf(moduleElement).getQualifiedName().toString();
            ModuleGen moduleGen = moduleElement.getAnnotation(ModuleGen.class);
            List<Element> fnElements = new ArrayList<>();
            List<FnImpl> fnImplElements = new ArrayList<>();
            List<? extends Element> classElements = elementUtils.getPackageOf(moduleElement).getEnclosedElements();
            for (Element classElement : classElements) {
                boolean isType = false;
                if (classElement instanceof TypeElement) {
                    isType = true;
                }
                if (!isType) {
                    continue;
                }
                TypeElement typeElement = (TypeElement) classElement;
                Fn fn = typeElement.getAnnotation(Fn.class);
                if (fn != null) {
                    fnElements.add(classElement);
                    continue;
                }

                List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
                if (interfaces == null || interfaces.isEmpty()) {
                    continue;
                }
                for (TypeMirror interface_ : interfaces) {
                    ClassName interfaceClassName = (ClassName) ClassName.get(interface_);
                    TypeElement interfaceTypeElement = elementUtils.getTypeElement(interfaceClassName.packageName() + "." + interfaceClassName.simpleName());
                    Fn interfaceFn = interfaceTypeElement.getAnnotation(Fn.class);
                    if (interfaceFn != null) {
                        fnImplElements.add(new FnImpl(interfaceTypeElement, typeElement, interfaceFn));
                        List<Element> fetchFnElements;
                        if (moduleFnMap.containsKey(moduleGen.name())) {
                            fetchFnElements = moduleFnMap.get(moduleGen.name());
                        } else {
                            fetchFnElements = new ArrayList<>();
                        }
                        fetchFnElements.add(interfaceTypeElement);
                        moduleFnMap.put(moduleGen.name(), fetchFnElements);
                        break;
                    }
                }
            }
            if (!fnElements.isEmpty()) {
                try {
                    new ModuleFnGenerator(this.messager, this.elementUtils, this.filer).generate(packageName, moduleGen, fnElements);
                } catch (Throwable exception) {
                    messager.printMessage(Diagnostic.Kind.ERROR, String.format("生成 %s:%s 模块的函数失败。", moduleGen.groupPackage(), moduleGen.name()));
                    messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
                    throw new RuntimeException(exception);
                }
            }
            if (!fnImplElements.isEmpty()) {
                try {
                    new ModuleImplGenerator(this.messager, this.elementUtils, this.filer).generate(packageName, moduleGen, fnImplElements);
                } catch (Throwable exception) {
                    messager.printMessage(Diagnostic.Kind.ERROR, String.format("生成 %s:%s 模块的函数失败。", moduleGen.groupPackage(), moduleGen.name()));
                    messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
                    throw new RuntimeException(exception);
                }
            }


        }

        return moduleFnMap;
    }

}
