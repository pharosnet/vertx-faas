package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.*;
import io.vertx.core.Vertx;
import org.pharosnet.vertx.faas.database.codegen.DatabaseType;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Map;

public class DALGenerator {

    public DALGenerator(ProcessingEnvironment processingEnv, Map<String, TableModel> tableModelMap, DatabaseType databaseType) {
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.tableModelMap = tableModelMap;
        this.databaseType = databaseType;
    }

    private final Map<String, TableModel> tableModelMap;
    private final Messager messager;
    private final Filer filer;
    private final Elements elementUtils;
    private final Types typeUtils;
    private final DatabaseType databaseType;

    public void generate(TypeElement element) throws Exception {
        DALModel dalModel = new DALModel(this.typeUtils, this.elementUtils, tableModelMap, element);
        this.generateDAL(dalModel);
    }

    private void generateDAL(DALModel dalModel) throws Exception {
        String pkg = dalModel.getClassName().packageName();


        // class
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(dalModel.getImplClassName())
                .addSuperinterface(
                        dalModel.getClassName()
                )
                .addModifiers(Modifier.PUBLIC);

        if (!dalModel.isView()) {
            typeBuilder
                    .superclass(
                            ParameterizedTypeName.get(
                                    ClassName.get("org.pharosnet.vertx.faas.database.api", "AbstractDALImpl"),
                                    dalModel.getTableClassName(),
                                    dalModel.getIdClassName()
                            )
                    );
        } else {
            typeBuilder
                    .superclass(
                            ParameterizedTypeName.get(
                                    ClassName.get("org.pharosnet.vertx.faas.database.api", "AbstractVALImpl"),
                                    dalModel.getTableClassName(),
                                    dalModel.getIdClassName()
                            )
                    );
        }

        // logger
        FieldSpec.Builder staticLogField = FieldSpec.builder(
                ClassName.get("org.slf4j", "Logger"), "log",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("$T.getLogger($T.class)",
                ClassName.get("org.slf4j", "LoggerFactory"),
                dalModel.getImplClassName());
        typeBuilder.addField(staticLogField.build());

        // construct
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Vertx.class, "vertx")
                .addStatement("super(vertx)");
        typeBuilder.addMethod(constructor.build());
        if (!dalModel.isView()) {
            this.generateCRUD(typeBuilder, dalModel);
        }

        for (DALMethodModel methodModel : dalModel.getMethodModels()) {
            this.generateMethod(typeBuilder, methodModel);
        }


        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder.build())
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
                .indent("\t")
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, dalModel.getImplClassName().simpleName()));
    }

    private void generateCRUD(TypeSpec.Builder typeBuilder, DALModel dalModel) {
        new DALGetGenerator(databaseType).generate(dalModel, typeBuilder);
        new DALInsertGenerator(databaseType).generate(dalModel, typeBuilder);
        new DALUpdateGenerator(databaseType).generate(dalModel, typeBuilder);
        new DALDeleteGenerator(databaseType).generate(dalModel, typeBuilder);
        new DALDeleteForceGenerator(databaseType).generate(dalModel, typeBuilder);
    }

    private void generateMethod(TypeSpec.Builder typeBuilder, DALMethodModel methodModel) {
        new DALMethodGenerator().generate(typeBuilder, methodModel);
    }

}
