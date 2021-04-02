package org.pharosnet.vertx.faas.database.codegen;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.TypeName;
import org.pharosnet.vertx.faas.database.codegen.annotations.DAL;
import org.pharosnet.vertx.faas.database.codegen.annotations.EnableDAL;
import org.pharosnet.vertx.faas.database.codegen.annotations.Table;
import org.pharosnet.vertx.faas.database.codegen.processor.DALGenerator;
import org.pharosnet.vertx.faas.database.codegen.processor.TableGenerator;
import org.pharosnet.vertx.faas.database.codegen.processor.TableModel;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
        "org.pharosnet.vertx.faas.database.codegen.annotations.EnableDAL",
        "org.pharosnet.vertx.faas.database.codegen.annotations.Table",
        "org.pharosnet.vertx.faas.database.codegen.annotations.DAL",
})
@SupportedOptions({"codegen.output"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class DatabaseCodeGenProcessor extends AbstractProcessor {

    private Messager messager;
    private Set<String> dals;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.dals = new HashSet<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            DatabaseType databaseType = this.getDatabaseType(roundEnv);
            if (databaseType == null) {
                return false;
            }
            Map<String, TableModel> tableModelMap = this.generateTable(roundEnv, databaseType);
            if (tableModelMap.isEmpty()) {
                throw new Exception("未能发现@Table的类。");
            }
            this.generateDAL(roundEnv, tableModelMap, databaseType);
        } catch (Exception e) {
            this.messager.printMessage(Diagnostic.Kind.ERROR, "" + e.getMessage());
            return false;
        }
        return true;
    }

    private DatabaseType getDatabaseType(RoundEnvironment roundEnv) throws Exception {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(EnableDAL.class);
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        if (elements.size() > 1) {
            throw new Exception("@EnableDAL 只能有一个.");
        }
        for (Element element: elements) {
            return element.getAnnotation(EnableDAL.class).type();
        }
        return null;
    }

    private Map<String, TableModel> generateTable(RoundEnvironment roundEnv, DatabaseType databaseType) throws Exception {
        Map<String, TableModel> tableModelMap = new HashMap<>();
        List<TypeElement> typeElements = roundEnv.getElementsAnnotatedWith(Table.class)
                .stream()
                .map(element -> (TypeElement) element)
                .collect(Collectors.toList());
        TableGenerator tableGenerator = new TableGenerator(this.processingEnv, databaseType);
        for (TypeElement element : typeElements) {
            if (this.dals.contains(TypeName.get(element.asType()).toString())) {
                continue;
            }
            TableModel tableModel = tableGenerator.generate(element);
            tableModelMap.put(tableModel.getClassName().packageName() + "." + tableModel.getClassName().simpleName(), tableModel);
            this.dals.add(TypeName.get(element.asType()).toString());
        }
        return tableModelMap;
    }

    private void generateDAL(RoundEnvironment roundEnv, Map<String, TableModel> tableModelMap, DatabaseType databaseType) throws Exception {
        List<TypeElement> typeElements = roundEnv.getElementsAnnotatedWith(DAL.class)
                .stream()
                .map(element -> (TypeElement) element)
                .collect(Collectors.toList());


        DALGenerator dalGenerator = new DALGenerator(processingEnv, tableModelMap, databaseType);
        for (TypeElement element : typeElements) {
            if (this.dals.contains(TypeName.get(element.asType()).toString())) {
                continue;
            }
            dalGenerator.generate(element);
            this.dals.add(TypeName.get(element.asType()).toString());
        }
    }
}