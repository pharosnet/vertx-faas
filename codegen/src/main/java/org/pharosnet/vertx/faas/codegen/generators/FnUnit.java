package org.pharosnet.vertx.faas.codegen.generators;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.pharosnet.vertx.faas.codegen.annotation.Fn;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;

public class FnUnit {

    public FnUnit() {
        this.parameters = new ArrayList<>();
    }

    private Fn fn;

    private String className;

    private String packageName;
    private TypeElement implTypeElement;

    private String methodName;
    private ParameterizedTypeName returnClass;
    private TypeName returnElementClass;

    private List<VariableElement> parameters;

    public Fn getFn() {
        return fn;
    }

    public void setFn(Fn fn) {
        this.fn = fn;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
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

    public TypeName getReturnElementClass() {
        return returnElementClass;
    }

    public void setReturnElementClass(TypeName returnElementClass) {
        this.returnElementClass = returnElementClass;
    }

    public List<VariableElement> getParameters() {
        return parameters;
    }

    public void setParameters(List<VariableElement> parameters) {
        this.parameters = parameters;
    }

    public TypeElement getImplTypeElement() {
        return implTypeElement;
    }

    public void setImplTypeElement(TypeElement implTypeElement) {
        this.implTypeElement = implTypeElement;
    }
}
