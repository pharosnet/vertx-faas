package org.pharosnet.vertx.faas.codegen.processor.generators;

import io.vertx.codegen.annotations.ModuleGen;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.List;

public class ModuleGenerator {

    public ModuleGenerator(Messager messager, Elements elementUtils, Filer filer) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.filer = filer;
    }

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;


    public void generate(String pkg, ModuleGen moduleGen, List<Element> fnElements) throws Exception {
        for (Element element : fnElements) {
            FnGenerator fnGenerator = new FnGenerator(this.messager, this.elementUtils, (TypeElement) element, moduleGen);
            fnGenerator.generate(this.filer);
        }
        // generate verticle
        VerticleGenerator verticleGenerator = new VerticleGenerator(this.messager, this.elementUtils, this.filer);
        verticleGenerator.generate(pkg, moduleGen);
        // generate deployment
        DeploymentGenerator deploymentGenerator = new DeploymentGenerator(this.messager, this.elementUtils, this.filer);
        deploymentGenerator.generate(pkg, moduleGen);

        // generate MessageConsumerRegister
        MessageConsumerRegisterGenerator messageConsumerRegisterGenerator = new MessageConsumerRegisterGenerator(this.messager, this.elementUtils, this.filer);
        messageConsumerRegisterGenerator.generate(pkg, moduleGen, fnElements);

    }

}
