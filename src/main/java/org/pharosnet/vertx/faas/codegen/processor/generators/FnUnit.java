package org.pharosnet.vertx.faas.codegen.processor.generators;

import com.squareup.javapoet.ParameterizedTypeName;
import org.pharosnet.vertx.faas.codegen.annotation.Fn;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class FnUnit {

    public FnUnit() {
        this.parameters = new ArrayList<>();
    }

    private Fn fn;

    private String className;

    private String packageName;

    private ParameterizedTypeName returnClass;
    private Class<?> returnElementClass;

    private List<Parameter> parameters;

    public Fn getFn() {
        return fn;
    }

    public void setFn(Fn fn) {
        this.fn = fn;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public ParameterizedTypeName getReturnClass() {
        return returnClass;
    }

    public void setReturnClass(ParameterizedTypeName returnClass) {
        this.returnClass = returnClass;
    }

    public Class<?> getReturnElementClass() {
        return returnElementClass;
    }

    public void setReturnElementClass(Class<?> returnElementClass) {
        this.returnElementClass = returnElementClass;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }
}
