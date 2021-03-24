package org.pharosnet.vertx.faas.codegen.processor;

import com.google.auto.service.AutoService;
import io.vertx.codegen.annotations.ModuleGen;
import org.pharosnet.vertx.faas.codegen.annotation.EnableOAS;
import org.pharosnet.vertx.faas.codegen.annotation.Fn;
import org.pharosnet.vertx.faas.codegen.processor.generators.ModuleGenerator;
import org.pharosnet.vertx.faas.codegen.processor.generators.OASGenerator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
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
            List<? extends Element> classElements = elementUtils.getPackageOf(moduleElement).getEnclosedElements();
            for (Element classElement : classElements) {
                Fn fn = classElement.getAnnotation(Fn.class);
                if (fn == null) {
                    continue;
                }
                fnElements.add(classElement);
            }
            if (fnElements.isEmpty()) {
                continue;
            }

            try {
                new ModuleGenerator(this.messager, this.elementUtils, this.filer).generate(packageName, moduleGen, fnElements);
            } catch (Throwable exception) {
                messager.printMessage(Diagnostic.Kind.ERROR, String.format("生成 %s:%s 模块的函数失败。", moduleGen.groupPackage(), moduleGen.name()));
                messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
                throw new RuntimeException(exception);
            }
            moduleFnMap.put(moduleGen.name(), fnElements);
        }

        return moduleFnMap;
    }

}
