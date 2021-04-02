package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.ClassName;
import io.vertx.codegen.format.CamelCase;
import org.pharosnet.vertx.faas.database.codegen.annotations.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

public class TableModel {

    public TableModel(Types typeUtils, TypeElement element) throws Exception {
        Table table = element.getAnnotation(Table.class);
        this.table = table;
        this.className = ClassName.get(element);
        this.mapperClassName = ClassName.get(this.className.packageName(), CamelCase.INSTANCE.format(List.of(this.className.simpleName(), "Mapper")));
        this.columnModels = scanFields(typeUtils, element);
        if (this.columnModels.isEmpty()) {
            throw new Exception(String.format("%s.%s 没有@Column属性", this.className.packageName(), this.className.simpleName()));
        }
    }

    private List<ColumnModel> scanFields(Types typeUtils, TypeElement element) throws Exception {
        List<ColumnModel> columnModels = new ArrayList<>();
        TypeMirror superclass = element.getSuperclass();
        if (superclass != null) {
            Element superclassElement = typeUtils.asElement(superclass);
            if (superclassElement instanceof TypeElement) {
                columnModels.addAll(this.scanFields(typeUtils, (TypeElement) superclassElement));
            }
        }
        List<? extends Element> enclosedElements = element.getEnclosedElements();
        if (enclosedElements == null) {
            return columnModels;
        }
        for (Element enclosedElement : enclosedElements) {
            if (!enclosedElement.getKind().equals(ElementKind.FIELD)) {
                continue;
            }
            VariableElement fieldElement = (VariableElement) enclosedElement;
            Column column = fieldElement.getAnnotation(Column.class);
            if (column == null) {
                continue;
            }
            ColumnKind columnKind;
            boolean needLastInsertedId = false;
            if (fieldElement.getAnnotation(Id.class) != null) {
                Id id = fieldElement.getAnnotation(Id.class);
                needLastInsertedId = id.needLastInsertedId();
                columnKind = ColumnKind.ID;
                this.needLastInsertedId = needLastInsertedId;

            } else if (fieldElement.getAnnotation(CreateBY.class) != null) {
                columnKind = ColumnKind.CREATE_BY;
            } else if (fieldElement.getAnnotation(CreateAT.class) != null) {
                columnKind = ColumnKind.CREATE_AT;
            } else if (fieldElement.getAnnotation(ModifyBY.class) != null) {
                columnKind = ColumnKind.MODIFY_BY;
            } else if (fieldElement.getAnnotation(ModifyAT.class) != null) {
                columnKind = ColumnKind.MODIFY_AT;
            } else if (fieldElement.getAnnotation(DeleteBY.class) != null) {
                columnKind = ColumnKind.DELETE_BY;
            } else if (fieldElement.getAnnotation(DeleteAT.class) != null) {
                columnKind = ColumnKind.DELETE_AT;
            } else if (fieldElement.getAnnotation(Version.class) != null) {
                columnKind = ColumnKind.VERSION;
            } else {
                columnKind = ColumnKind.NORMAL;
            }
            String fieldName = fieldElement.getSimpleName().toString();
            ClassName fieldClassName = (ClassName) ClassName.get(fieldElement.asType());
            ColumnModel columnModel = new ColumnModel();
            columnModel.setFieldName(fieldName);
            columnModel.setClassName(fieldClassName);
            columnModel.setElement(fieldElement);
            columnModel.setColumn(column);
            columnModel.setKind(columnKind);
            columnModel.setNeedLastInsertedId(needLastInsertedId);

            columnModels.add(columnModel);
        }
        return columnModels;
    }


    private ClassName className;
    private Table table;
    private List<ColumnModel> columnModels;

    private ClassName mapperClassName;

    private boolean needLastInsertedId;

    public ClassName getClassName() {
        return className;
    }

    public void setClassName(ClassName className) {
        this.className = className;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public List<ColumnModel> getColumnModels() {
        return columnModels;
    }

    public void setColumnModels(List<ColumnModel> columnModels) {
        this.columnModels = columnModels;
    }

    public ClassName getMapperClassName() {
        return mapperClassName;
    }

    public void setMapperClassName(ClassName mapperClassName) {
        this.mapperClassName = mapperClassName;
    }

    public boolean isNeedLastInsertedId() {
        return needLastInsertedId;
    }

    public void setNeedLastInsertedId(boolean needLastInsertedId) {
        this.needLastInsertedId = needLastInsertedId;
    }
}
