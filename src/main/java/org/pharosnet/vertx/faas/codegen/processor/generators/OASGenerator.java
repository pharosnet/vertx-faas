package org.pharosnet.vertx.faas.codegen.processor.generators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.pharosnet.vertx.faas.codegen.annotation.EnableOAS;
import org.pharosnet.vertx.faas.codegen.annotation.oas.ServerVariable;
import org.pharosnet.vertx.faas.codegen.annotation.oas.Tag;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OASGenerator {

    public OASGenerator(Messager messager, Elements elementUtils, Filer filer) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.filer = filer;
    }

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;


    public void generate(Map<String, List<Element>> moduleFnMap, EnableOAS enableOAS) throws Exception {
        String resourcePath = enableOAS.resourcePath().trim();
        if (resourcePath.length() == 0) {
            throw new Exception("构建OpenAPI失败，resourcePath不能为空！");
        }
        String resourcePkg = resourcePath.substring(0, resourcePath.lastIndexOf("/")).replaceAll("/", ".");
        String resourceName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);

        FileObject dataFile;

        try {
            dataFile = this.filer.getResource(StandardLocation.SOURCE_PATH, resourcePkg, resourceName);
            // delete old one
            dataFile.delete();

            dataFile = this.filer.createResource(StandardLocation.SOURCE_OUTPUT, resourcePkg, resourceName);
        } catch (FileNotFoundException fileNotFoundException) {
            dataFile = this.filer.createResource(StandardLocation.SOURCE_OUTPUT, resourcePkg, resourceName);
        }

        try {

            OpenAPI openAPI = this.buildOpenAPI(moduleFnMap, enableOAS);

            // write new one
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            String jsonData = mapper.writer(new DefaultPrettyPrinter()).writeValueAsString(openAPI);

            Writer writer = dataFile.openWriter();
            writer.write(jsonData);
            writer.close();
            this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 OPENAPI %s", dataFile.toUri().toString()));

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("构建OpenAPI失败，写入resource文件失败！", e);
        }


    }

    private OpenAPI buildOpenAPI(Map<String, List<Element>> moduleFnMap, EnableOAS enableOAS) throws Exception {
        OpenAPI openAPI = new OpenAPI();
        // info
        Info info = new Info();
        info.title(enableOAS.info().title());
        info.description(enableOAS.info().description());
        info.termsOfService(enableOAS.info().termsOfService());
        info.contact(new Contact()
                .email(enableOAS.info().contact().email())
                .name(enableOAS.info().contact().name())
                .url(enableOAS.info().contact().url()));
        info.license(new License()
                .name(enableOAS.info().license().name())
                .url(enableOAS.info().license().url()));
        info.version(enableOAS.info().version());
        info.extensions(Map.of("latency", "x-response-time", "requestId", "x-request-id"));

        openAPI.info(info);

        // servers
        List<Server> servers = new ArrayList<>();

        for (org.pharosnet.vertx.faas.codegen.annotation.oas.Server server : enableOAS.servers()) {
            ServerVariables variables = new ServerVariables();
            for (ServerVariable variable : server.variables()) {
                variables.addServerVariable(variable.name(),
                        new io.swagger.v3.oas.models.servers.ServerVariable()
                                .description(variable.description())
                                ._default(variable._default())
                                ._enum(List.of(variable._enum())));
            }
            servers.add(new Server().url(server.url()).description(server.name()).variables(variables));
        }
        openAPI.servers(servers);

        // paths
        OASPathsGenerator pathsGenerator = new OASPathsGenerator(this.elementUtils);
        openAPI.setPaths(pathsGenerator.generate(moduleFnMap));

        // components
        openAPI.components(new Components().schemas(pathsGenerator.getSchemas()));

        // security

        // tags
        for (Tag tag : enableOAS.tags()) {
            openAPI.addTagsItem(new io.swagger.v3.oas.models.tags.Tag()
                    .name(tag.name())
                    .description(tag.description()));
        }

        return openAPI;
    }


}
