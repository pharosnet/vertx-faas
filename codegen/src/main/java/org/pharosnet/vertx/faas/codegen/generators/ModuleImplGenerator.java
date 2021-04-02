package org.pharosnet.vertx.faas.codegen.generators;

import org.pharosnet.vertx.faas.codegen.annotation.FnModule;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import java.util.List;

public class ModuleImplGenerator {

    public ModuleImplGenerator(Messager messager, Elements elementUtils, Filer filer) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.filer = filer;
    }

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;


    public void generate(String pkg, FnModule fnModule, List<FnImpl> fnImpls) throws Exception {
        for (FnImpl fnImpl : fnImpls) {
            FnImplGenerator fnImplGenerator = new FnImplGenerator(this.messager, this.elementUtils, fnImpl);
            fnImplGenerator.generate(this.filer);
        }
        // generate verticle
        VerticleGenerator verticleGenerator = new VerticleGenerator(this.messager, this.elementUtils, this.filer);
        verticleGenerator.generate(pkg, fnModule);
        // generate deployment
        DeploymentGenerator deploymentGenerator = new DeploymentGenerator(this.messager, this.elementUtils, this.filer);
        deploymentGenerator.generate(pkg, fnModule);

        // generate MessageConsumerRegister
        MessageConsumerRegisterGenerator messageConsumerRegisterGenerator = new MessageConsumerRegisterGenerator(this.messager, this.elementUtils, this.filer);
        messageConsumerRegisterGenerator.generate(pkg, fnModule, fnImpls);

    }

}
