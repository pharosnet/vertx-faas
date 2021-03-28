package org.pharosnet.vertx.faas.codegen.generators;

import org.pharosnet.vertx.faas.codegen.annotation.Fn;

import javax.lang.model.element.TypeElement;

public class FnImpl {

    public FnImpl(TypeElement interfaceTypeElement, TypeElement typeElement, Fn fn) {
        this.interfaceTypeElement = interfaceTypeElement;
        this.typeElement = typeElement;
        this.fn = fn;
    }

    private TypeElement interfaceTypeElement;
    private TypeElement typeElement;
    private Fn fn;

    public TypeElement getInterfaceTypeElement() {
        return interfaceTypeElement;
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

    public Fn getFn() {
        return fn;
    }

    public void setFn(Fn fn) {
        this.fn = fn;
    }
}
