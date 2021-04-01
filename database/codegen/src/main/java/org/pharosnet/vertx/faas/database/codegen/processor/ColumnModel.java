package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.ClassName;
import org.pharosnet.vertx.faas.database.codegen.annotations.Column;

public class ColumnModel {

    public ColumnModel() {
    }

    private String fieldName;
    private ClassName className;
    private Column column;
    private ColumnKind kind;
    private boolean needLastInsertedId;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public ClassName getClassName() {
        return className;
    }

    public void setClassName(ClassName className) {
        this.className = className;
    }

    public Column getColumn() {
        return column;
    }

    public void setColumn(Column column) {
        this.column = column;
    }

    public ColumnKind getKind() {
        return kind;
    }

    public void setKind(ColumnKind kind) {
        this.kind = kind;
    }

    public boolean isNeedLastInsertedId() {
        return needLastInsertedId;
    }

    public void setNeedLastInsertedId(boolean needLastInsertedId) {
        this.needLastInsertedId = needLastInsertedId;
    }
}
