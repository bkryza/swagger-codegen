package io.swagger.codegen.languages;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.DefaultCodegen;
import io.swagger.codegen.SupportingFile;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.Swagger;
import io.swagger.util.Yaml;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EspHttpdServerCodegen extends DefaultCodegen implements CodegenConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(EspHttpdServerCodegen.class);
    protected String gemName;
    protected String moduleName;
    protected String libFolder = "lib";

    public EspHttpdServerCodegen() {
        super();
        outputFolder = "generated-code" + File.separator + "esphttpd";

        // no model
        modelTemplateFiles.clear();
        //modelTemplateFiles.put("rest_model_header.mustache", ".h");
        //modelTemplateFiles.put("rest_model_impl.mustache", ".c");

        apiTemplateFiles.put("rest_api_impl.mustache", ".c");
        apiTemplateFiles.put("rest_api_header.mustache", ".h");

        embeddedTemplateDir = templateDir = "./";

/*
auto    else    long    switch
break   enum    register    typedef
case    extern  return  union
char    float   short   unsigned
const   for signed  void
continue    goto    sizeof  volatile
default if  static  while
do  int struct  _Packed
double       
 */
        setReservedWordsLowerCase(
                Arrays.asList(
                    "auto", "else", "long", "switch",
                    "break", "enum", "register", "typedef",
                    "case", "extern", "return", "union"
                    )
        );


        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList(
                        "bool",
                        "qint32",
                        "qint64",
                        "float",
                        "double")
        );

        super.typeMapping = new HashMap<String, String>();

        typeMapping.put("Date", "time_t");
        typeMapping.put("DateTime", "time_t");
        typeMapping.put("string", "const char*");
        typeMapping.put("integer", "int32_t");
        typeMapping.put("long", "int32_t");
        typeMapping.put("boolean", "bool");
        typeMapping.put("array", "TLList");
        typeMapping.put("map", "TLMap");
        typeMapping.put("file", "char*");

        // remove modelPackage and apiPackage added by default
        cliOptions.clear();
    }

    @Override
    public void processOpts() {
        super.processOpts();

        // use constant model/api package (folder path)
        //setModelPackage("include");
        //setApiPackage("user");

        supportingFiles.add(new SupportingFile("rest_model_header.mustache", "", "rest_model.h"));
        supportingFiles.add(new SupportingFile("rest_model_impl.mustache", "", "rest_model.c"));

        //supportingFiles.add(new SupportingFile("rest_api.mustache", "", "rest_api.erl"));

        //supportingFiles.add(new SupportingFile("Swaggering.rb", libFolder, "swaggering.rb"));
        //supportingFiles.add(new SupportingFile("config.ru", "", "config.ru"));
        //supportingFiles.add(new SupportingFile("Gemfile", "", "Gemfile"));
        //supportingFiles.add(new SupportingFile("README.md", "", "README.md"));
        //supportingFiles.add(new SupportingFile("swagger.mustache","","swagger.yaml"));
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "esphttpd";
    }

    @Override
    public String getHelp() {
        return "Generates an Esp httpd compatible REST server library.";
    }

    @Override
    public String escapeReservedWord(String name) {
        return "_" + name;
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + apiPackage.replace("/", File.separator);
    }

    @Override
    public String getTypeDeclaration(Property p) {
        if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            return getSwaggerType(p) + "[" + getTypeDeclaration(inner) + "]";
        } else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();
            return getSwaggerType(p) + "[string," + getTypeDeclaration(inner) + "]";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type)) {
                return type;
            }
        } else {
            type = swaggerType;
        }
        if (type == null) {
            return null;
        }
        return type;
    }

    @Override
    public String toDefaultValue(Property p) {
        return "null";
    }

    @Override
    public String toVarName(String name) {
        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // if it's all uppper case, convert to lower case
        if (name.matches("^[A-Z_]*$")) {
            name = name.toLowerCase();
        }

        // camelize (lower first character) the variable name
        // petId => pet_id
        name = underscore(name);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    @Override
    public String toParamName(String name) {
        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String toModelName(String name) {
        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            throw new RuntimeException(name + " (reserved word) cannot be used as a model name");
        }

        // camelize the model name
        // phone_number => PhoneNumber
        return name;
    }

    @Override
    public String toModelFilename(String name) {
        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            throw new RuntimeException(name + " (reserved word) cannot be used as a model name");
        }

        // underscore the model file name
        // PhoneNumber.rb => phone_number.rb
        return underscore(name);
    }

    @Override
    public String toApiFilename(String name) {
        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // e.g. PhoneNumberApi.rb => phone_number_api.rb
        return underscore(name) + "_api";
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }
        // e.g. phone_number_api => PhoneNumberApi
        return camelize(name) + "Api";
    }

    @Override
    public String toOperationId(String operationId) {
        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            throw new RuntimeException(operationId + " (reserved word) cannot be used as method name");
        }

        return underscore(operationId);
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        Swagger swagger = (Swagger)objs.get("swagger");
        if(swagger != null) {
            try {
                objs.put("swagger-yaml", Yaml.mapper().writeValueAsString(swagger));
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return super.postProcessSupportingFileData(objs);
    }

}
