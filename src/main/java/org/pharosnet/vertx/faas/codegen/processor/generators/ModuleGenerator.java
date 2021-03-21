package org.pharosnet.vertx.faas.codegen.processor.generators;

import javax.annotation.processing.Filer;
import java.util.List;

public class ModuleGenerator {

    public ModuleGenerator(List<FnUnit> fnUnits) {
        this.fnUnits = fnUnits;
    }

    private final List<FnUnit> fnUnits;

    public void generate(Filer filer) throws Exception {

    }

}
