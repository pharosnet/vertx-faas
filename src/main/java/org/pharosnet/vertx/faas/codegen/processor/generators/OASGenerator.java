package org.pharosnet.vertx.faas.codegen.processor.generators;

import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.codegen.annotations.ModuleGen;
import org.pharosnet.vertx.faas.codegen.annotation.EnableOAS;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.Map;

public class OASGenerator {

    public OASGenerator(Messager messager, Elements elementUtils, Filer filer) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.filer = filer;
    }

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;



    public void generate(Map<String, List<Element>> moduleFnMap, EnableOAS enableOAS) throws Exception {


        // delete old one
//        this.filer.getResource().delete();

        OpenAPI openAPI = new OpenAPI();
        //
//        this.filer.createResource()
    }

}
