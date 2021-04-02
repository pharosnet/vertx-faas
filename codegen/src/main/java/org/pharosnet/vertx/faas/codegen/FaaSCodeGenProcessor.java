package org.pharosnet.vertx.faas.codegen;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import io.vertx.codegen.annotations.DataObject;
import org.pharosnet.vertx.faas.codegen.annotation.Fn;
import org.pharosnet.vertx.faas.codegen.annotation.FnInterceptor;
import org.pharosnet.vertx.faas.codegen.annotation.FnModule;
import org.pharosnet.vertx.faas.codegen.generators.*;
import org.pharosnet.vertx.faas.core.annotations.EnableOAS;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;

@SupportedAnnotationTypes({
        "org.pharosnet.vertx.faas.codegen.annotation.EnableOAS",
        "org.pharosnet.vertx.faas.codegen.annotation.FnModule",
        "org.pharosnet.vertx.faas.codegen.annotation.Fn",
})
@SupportedOptions({"codegen.output"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class FaaSCodeGenProcessor extends AbstractProcessor {

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;
    private Types typeUtils;

    private Set<String> fns;
    private Set<String> fnImpls;
    private Set<String> fnModules;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.typeUtils = processingEnv.getTypeUtils();
        this.fns = new HashSet<>();
        this.fnImpls = new HashSet<>();
        this.fnModules = new HashSet<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        this.generateDataObjectPlus(roundEnv);
        this.generateModuleFn(roundEnv);
        Map<String, List<Element>> moduleFnMap = this.generateModuleFnImpl(roundEnv);
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

    private void generateModuleFn(RoundEnvironment roundEnv) {

        Set<? extends Element> fnElements = roundEnv.getElementsAnnotatedWith(Fn.class);
        if (fnElements == null || fnElements.isEmpty()) {
            return;
        }
        for (Element fnElement : fnElements) {
            if (this.fns.contains(fnElement.getSimpleName().toString())) {
                continue;
            }
            String packageName = elementUtils.getPackageOf(fnElement).getQualifiedName().toString();
            try {
                FnGenerator fnGenerator = new FnGenerator(this.messager, this.elementUtils, (TypeElement) fnElement);
                fnGenerator.generate(this.filer);
                this.fns.add(fnElement.getSimpleName().toString());
            } catch (Throwable exception) {
                messager.printMessage(Diagnostic.Kind.ERROR, String.format("生成 %s:%s 函数失败。", packageName, fnElement.getSimpleName().toString()));
                messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
                throw new RuntimeException(exception);
            }
        }
    }

    private Map<String, List<Element>> generateModuleFnImpl(RoundEnvironment roundEnv) {
        Map<String, List<Element>> moduleFnMap = new HashMap<>();
        Set<? extends Element> moduleElements = roundEnv.getElementsAnnotatedWith(FnModule.class);
        if (moduleElements == null || moduleElements.isEmpty()) {
            return moduleFnMap;
        }
        for (Element moduleElement : moduleElements) {
            String packageName = elementUtils.getPackageOf(moduleElement).getQualifiedName().toString();
            FnModule fnModule = moduleElement.getAnnotation(FnModule.class);


            if (this.fnModules.contains(packageName)) {
                continue;
            }


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

                List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
                if (interfaces == null || interfaces.isEmpty()) {
                    continue;
                }
                for (TypeMirror interface_ : interfaces) {
                    Element interfaceElement = this.typeUtils.asElement(interface_);
                    Fn interfaceFn = interfaceElement.getAnnotation(Fn.class);
                    if (interfaceFn != null) {
                        String fnName = ClassName.get(typeElement).packageName() + "." + ClassName.get(typeElement).simpleName();
                        if (this.fnImpls.contains(fnName)) {
                            break;
                        }
                        this.fnImpls.add(fnName);
                        FnInterceptor fnInterceptor = typeElement.getAnnotation(FnInterceptor.class);
                        fnImplElements.add(new FnImpl((TypeElement) interfaceElement, typeElement, fnModule, fnInterceptor));
                        List<Element> fetchFnElements;
                        if (moduleFnMap.containsKey(fnModule.name())) {
                            fetchFnElements = moduleFnMap.get(fnModule.name());
                        } else {
                            fetchFnElements = new ArrayList<>();
                        }
                        fetchFnElements.add(interfaceElement);
                        moduleFnMap.put(fnModule.name(), fetchFnElements);
                        break;
                    }
                }
            }
            if (!fnImplElements.isEmpty()) {
                try {
                    new ModuleImplGenerator(this.messager, this.elementUtils, this.filer).generate(packageName, fnModule, fnImplElements);
                } catch (Throwable exception) {
                    messager.printMessage(Diagnostic.Kind.ERROR, String.format("生成 %s 模块的函数失败。", fnModule.name()));
                    messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
                    throw new RuntimeException(exception);
                }
            }
            this.fnModules.add(packageName);
        }

        return moduleFnMap;
    }

    private void generateDataObjectPlus(RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(DataObject.class);
        if (elements == null || elements.isEmpty()) {
            return;
        }
        DataObjectPlusGenerator generator = new DataObjectPlusGenerator(this.messager, this.elementUtils, this.typeUtils, this.filer);
        for (Element element : elements) {
            try {
                generator.generate(element);
            } catch (Throwable exception) {
                messager.printMessage(Diagnostic.Kind.ERROR, String.format("生成 %s DataObject Json Mapper失败。", element.getSimpleName().toString()));
                messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
                throw new RuntimeException(exception);
            }

        }
    }

}
