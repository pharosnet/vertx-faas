package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.ClassName;
import org.pharosnet.vertx.faas.database.codegen.annotations.Arg;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class DALMethodParamModel {

    public DALMethodParamModel(Types typeUtils, VariableElement parameter) throws Exception {
        this.arg = parameter.getAnnotation(Arg.class);
        if (this.arg != null) {
            if (!this.arg.placeholder() && this.arg.value().length == 0) {
                throw new Exception(String.format("%s 参数的@Arg必须设置value或placeholder", parameter.getSimpleName()));
            }
        }
        this.paramName = parameter.getSimpleName().toString();
        this.paramTypeElement = (TypeElement) typeUtils.asElement(parameter.asType());
        this.paramClassName = ClassName.get(this.paramTypeElement);
    }


    private Arg arg;
    private TypeElement paramTypeElement;
    private ClassName paramClassName;
    private String paramName;

    public Arg getArg() {
        return arg;
    }

    public void setArg(Arg arg) {
        this.arg = arg;
    }

    public TypeElement getParamTypeElement() {
        return paramTypeElement;
    }

    public void setParamTypeElement(TypeElement paramTypeElement) {
        this.paramTypeElement = paramTypeElement;
    }

    public ClassName getParamClassName() {
        return paramClassName;
    }

    public void setParamClassName(ClassName paramClassName) {
        this.paramClassName = paramClassName;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }
}
