package org.pharosnet.vertx.faas.codegen.generators;

import org.pharosnet.vertx.faas.codegen.annotation.FnInterceptor;
import org.pharosnet.vertx.faas.codegen.annotation.FnModule;

import javax.lang.model.element.TypeElement;

public class FnImpl {

    public FnImpl(TypeElement interfaceTypeElement, TypeElement typeElement, FnModule fnModule, FnInterceptor fnInterceptor) {
        this.interfaceTypeElement = interfaceTypeElement;
        this.typeElement = typeElement;
        this.fnInterceptor = fnInterceptor;
        this.fnModule = fnModule;
    }

    private String pkg;
    private String className;
    private FnUnit fnUnit;
    private TypeElement interfaceTypeElement;
    private TypeElement typeElement;
    private FnModule fnModule;
    private FnInterceptor fnInterceptor;

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public TypeElement getInterfaceTypeElement() {
        return interfaceTypeElement;
    }

    public FnUnit getFnUnit() {
        return fnUnit;
    }

    public void setFnUnit(FnUnit fnUnit) {
        this.fnUnit = fnUnit;
    }

    public void setInterfaceTypeElement(TypeElement interfaceTypeElement) {
        this.interfaceTypeElement = interfaceTypeElement;
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public void setTypeElement(TypeElement typeElement) {
        this.typeElement = typeElement;
    }

    public FnModule getFnModule() {
        return fnModule;
    }

    public void setFnModule(FnModule fnModule) {
        this.fnModule = fnModule;
    }

    public FnInterceptor getFnInterceptor() {
        return fnInterceptor;
    }

    public void setFnInterceptor(FnInterceptor fnInterceptor) {
        this.fnInterceptor = fnInterceptor;
    }
}
