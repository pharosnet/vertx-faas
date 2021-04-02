package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.pharosnet.vertx.faas.database.codegen.annotations.Arg;
import org.pharosnet.vertx.faas.database.codegen.annotations.Query;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import java.util.*;

public class DALMethodModel {

    public DALMethodModel(Types types, ExecutableElement methodElement, int pos) throws Exception {
        this.methodElement = methodElement;
        this.name = methodElement.getSimpleName().toString();
        this.query = methodElement.getAnnotation(Query.class);
        this.sqlFieldName = String.format("_%sSQL%d", this.name, pos);

        TypeName returnTypeName = TypeName.get(methodElement.getReturnType());
        if (returnTypeName instanceof ParameterizedTypeName) {
            this.returnClassName = ((ParameterizedTypeName) returnTypeName);
            if (!this.returnClassName.rawType.toString().equals("io.vertx.core.Future")) {
                throw new Exception(String.format("%s 函数的返回值不是io.vertx.core.Future。", methodElement.getSimpleName()));
            }
            this.returnTopParameterizedTypeName = this.returnClassName.typeArguments.get(0);
            this.loadReturnElementClassName(this.returnTopParameterizedTypeName);
        } else {
            throw new Exception(String.format("%s 函数的返回值不是io.vertx.core.Future。", methodElement.getSimpleName()));
        }
        List<? extends VariableElement> parameters = methodElement.getParameters();
        if (parameters == null || parameters.size() == 0) {
            throw new Exception(String.format("%s 函数的参数为空。", methodElement.getSimpleName()));
        }
        this.paramModels = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement parameter = parameters.get(i);
            if (i == 0) {
                if (!TypeName.get(parameter.asType()).toString().equals("org.pharosnet.vertx.faas.database.api.SqlContext")) {
                    throw new Exception(String.format("%s 函数的第一个参数不是org.pharosnet.vertx.faas.database.api.SqlContext。", methodElement.getSimpleName()));
                }
            } else {
                if (parameter.getAnnotation(Arg.class) == null) {
                    throw new Exception(String.format("%s 函数参数没有@Arg。", methodElement.getSimpleName()));
                }
            }
            this.paramModels.add(new DALMethodParamModel(types, parameter));
        }
        if (this.paramModels.isEmpty()) {
            throw new Exception(String.format("%s 函数没有参数。", methodElement.getSimpleName()));
        }
        List<QueryArg> args = new ArrayList<>();
        for (DALMethodParamModel paramModel : this.paramModels) {
            if (paramModel.getArg() == null) {
                continue;
            }
            if (paramModel.getArg().placeholder()) {
                if (this.placeholders == null) {
                    this.placeholders = new HashMap<>();
                }
                String placeholderCode;
                TypeName paramModelTypeName = TypeName.get(paramModel.getParamTypeElement().asType());
                if (paramModelTypeName instanceof ParameterizedTypeName) {
                    ParameterizedTypeName paramModelTypeName0 = (ParameterizedTypeName) paramModelTypeName;
                    if (!paramModelTypeName0.rawType.toString().equals("java.util.List")) {
                        throw new Exception(String.format("%s 函数的placeholder arg必须是java.util.List。", methodElement.getSimpleName()));
                    }
                    TypeName placeholderArgTypeName = paramModelTypeName0.typeArguments.get(0);
                    if (placeholderArgTypeName.toString().contains("Integer")) {
                        placeholderCode = paramModel.getParamName() + ".stream().map(v -> String.format(\"%s\", v)).reduce((v1, v2) -> String.format(\"%s, %s\", v1, v2)).orElse(\"NULL\");\n";
                    } else if (placeholderArgTypeName.toString().contains("Double")) {
                        placeholderCode = paramModel.getParamName() + ".stream().map(v -> String.format(\"%s\", v)).reduce((v1, v2) -> String.format(\"%s, %s\", v1, v2)).orElse(\"NULL\");\n";
                    } else if (placeholderArgTypeName.toString().contains("Long")) {
                        placeholderCode = paramModel.getParamName() + ".stream().map(v -> String.format(\"%s\", v)).reduce((v1, v2) -> String.format(\"%s, %s\", v1, v2)).orElse(\"NULL\");\n";
                    } else {
                        placeholderCode = paramModel.getParamName() + ".stream().map(v -> String.format(\"%s\", v.toString())).reduce((v1, v2) -> String.format(\"%s, %s\", v1, v2)).orElse(\"NULL\");\n";
                    }
                    this.placeholders.put(paramModel.getParamName(), placeholderCode);
                } else {
                    throw new Exception(String.format("%s 函数的placeholder arg必须是java.util.List。", methodElement.getSimpleName()));
                }
                if (!this.hasPlaceholder) {
                    this.hasPlaceholder = true;
                }
                continue;
            }

            for (int ppos : paramModel.getArg().value()) {
                args.add(new QueryArg(ppos, paramModel.getParamName()));
            }

            this.paramArgNames = new ArrayList<>();

            args.sort(Comparator.comparingInt(QueryArg::getPos));
            for (QueryArg arg : args) {
                this.paramArgNames.add(arg.getName());
            }
        }
    }

    private void loadReturnElementClassName(TypeName returnTypeName) throws Exception {
        if (returnTypeName instanceof ParameterizedTypeName) {
            ParameterizedTypeName returnParameterParameterizedTypeName = (ParameterizedTypeName) returnTypeName;
            if (returnParameterParameterizedTypeName.rawType.toString().equals("java.util.Optional")) {
                this.returnWithOptional = true;
                TypeName returnParameterTypeName = returnParameterParameterizedTypeName.typeArguments.get(0);
                if (returnParameterTypeName instanceof ParameterizedTypeName) {
                    ParameterizedTypeName layer2 = (ParameterizedTypeName) returnParameterTypeName;
                    if (layer2.rawType.toString().contains("java.util.List")) {
                        this.singleReturn = false;
                        this.returnList = true;
                        this.returnElementClassName = layer2.typeArguments.get(0);
                    } else if (layer2.rawType.toString().contains("java.util.stream.Stream")) {
                        this.singleReturn = false;
                        this.returnStream = true;
                        this.returnElementClassName = layer2.typeArguments.get(0);

                    } else {
                        throw new Exception(String.format("%s 函数的返回值的泛型不合法。", methodElement.getSimpleName()));
                    }
                } else {
                    this.singleReturn = true;
                    this.returnElementClassName = returnParameterTypeName;
                }
            }
        } else {
            this.singleReturn = true;
            this.returnElementClassName = returnTypeName;
        }
    }

    private String name;
    private Query query;
    private String sqlFieldName;
    private ParameterizedTypeName returnClassName;
    private TypeName returnElementClassName;
    private List<DALMethodParamModel> paramModels;
    private List<String> paramArgNames;
    private boolean hasPlaceholder;
    private Map<String, String> placeholders;
    private ExecutableElement methodElement;
    private boolean singleReturn;
    private TypeName returnTopParameterizedTypeName;
    private boolean returnWithOptional;
    private boolean returnList;
    private boolean returnStream;


    public ExecutableElement getMethodElement() {
        return methodElement;
    }

    public void setMethodElement(ExecutableElement methodElement) {
        this.methodElement = methodElement;
    }

    public String getSqlFieldName() {
        return sqlFieldName;
    }

    public void setSqlFieldName(String sqlFieldName) {
        this.sqlFieldName = sqlFieldName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public ParameterizedTypeName getReturnClassName() {
        return returnClassName;
    }

    public void setReturnClassName(ParameterizedTypeName returnClassName) {
        this.returnClassName = returnClassName;
    }

    public List<DALMethodParamModel> getParamModels() {
        return paramModels;
    }

    public void setParamModels(List<DALMethodParamModel> paramModels) {
        this.paramModels = paramModels;
    }

    public TypeName getReturnElementClassName() {
        return returnElementClassName;
    }

    public List<String> getParamArgNames() {
        return paramArgNames;
    }

    public void setParamArgNames(List<String> paramArgNames) {
        this.paramArgNames = paramArgNames;
    }

    public boolean isHasPlaceholder() {
        return hasPlaceholder;
    }

    public void setHasPlaceholder(boolean hasPlaceholder) {
        this.hasPlaceholder = hasPlaceholder;
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    public boolean isSingleReturn() {
        return singleReturn;
    }

    public void setSingleReturn(boolean singleReturn) {
        this.singleReturn = singleReturn;
    }

    public void setReturnElementClassName(TypeName returnElementClassName) {
        this.returnElementClassName = returnElementClassName;
    }

    public TypeName getReturnTopParameterizedTypeName() {
        return returnTopParameterizedTypeName;
    }

    public void setReturnTopParameterizedTypeName(TypeName returnTopParameterizedTypeName) {
        this.returnTopParameterizedTypeName = returnTopParameterizedTypeName;
    }

    public boolean isReturnWithOptional() {
        return returnWithOptional;
    }

    public void setReturnWithOptional(boolean returnWithOptional) {
        this.returnWithOptional = returnWithOptional;
    }

    public boolean isReturnList() {
        return returnList;
    }

    public void setReturnList(boolean returnList) {
        this.returnList = returnList;
    }

    public boolean isReturnStream() {
        return returnStream;
    }

    public void setReturnStream(boolean returnStream) {
        this.returnStream = returnStream;
    }
}
