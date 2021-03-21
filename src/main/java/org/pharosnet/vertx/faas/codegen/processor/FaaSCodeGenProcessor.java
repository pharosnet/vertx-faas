package org.pharosnet.vertx.faas.codegen.processor;

import com.google.auto.service.AutoService;
import org.pharosnet.vertx.faas.codegen.annotation.Fn;
import org.pharosnet.vertx.faas.codegen.processor.generators.FnGenerator;
import org.pharosnet.vertx.faas.codegen.processor.generators.FnUnit;
import org.pharosnet.vertx.faas.codegen.processor.generators.ModuleGenerator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
        "org.pharosnet.vertx.faas.codegen.annotation.Fn",
})
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
        return this.generateFn(roundEnv);
    }

    private boolean generateFn(RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Fn.class);
        if (elements == null || elements.isEmpty()) {
            return true;
        }

        List<FnGenerator> generators = elements
                .stream()
                .map(e -> {
                    try {
                        return new FnGenerator(this.messager, elementUtils, (TypeElement) e);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
                        throw new RuntimeException(exception);
                    }
                }).collect(Collectors.toList());

        List<FnUnit> fnUnits = new ArrayList<>();
        for (FnGenerator generator : generators) {
            try {
                fnUnits.add(generator.generate(filer));
            } catch (Exception cause) {
                cause.printStackTrace();
                messager.printMessage(Diagnostic.Kind.ERROR, cause.getMessage());
                return false;
            }
        }

        // 生成fn的 vertile 和 deployment
        try {
            ModuleGenerator moduleGenerator = new ModuleGenerator(fnUnits);
            moduleGenerator.generate(filer);
        } catch (Exception cause) {
            cause.printStackTrace();
            messager.printMessage(Diagnostic.Kind.ERROR, cause.getMessage());
            return false;
        }

        return true;
    }


}
