package swagger.grails4.openapi


import com.thoughtworks.paranamer.BytecodeReadingParanamer
import com.thoughtworks.paranamer.CachingParanamer
import com.thoughtworks.paranamer.Paranamer
import grails.core.DefaultGrailsApplication
import grails.core.GrailsControllerClass
import grails.web.mapping.UrlCreator
import grails.web.mapping.UrlMapping
import grails.web.mapping.UrlMappingsHolder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.swagger.v3.oas.annotations.Operation as OperationAnnotation
import io.swagger.v3.oas.annotations.Parameter as ParameterAnnotation
import io.swagger.v3.oas.annotations.media.Content as ContentAnnotation
import io.swagger.v3.oas.annotations.media.Schema as SchemaAnnotation
import io.swagger.v3.oas.annotations.responses.ApiResponse as ResponseAnnotation
import io.swagger.v3.oas.annotations.tags.Tag as TagAnnotation
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration
import io.swagger.v3.oas.integration.api.OpenApiReader
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation as OperationModel
import io.swagger.v3.oas.models.PathItem as PathItemModel
import io.swagger.v3.oas.models.Paths as PathsModel
import io.swagger.v3.oas.models.media.Content as ContentModel
import io.swagger.v3.oas.models.media.MediaType as MediaTypeModel
import io.swagger.v3.oas.models.media.Schema as SchemaModel
import io.swagger.v3.oas.models.parameters.Parameter as ParameterModel
import io.swagger.v3.oas.models.responses.ApiResponse as ResponseModel
import io.swagger.v3.oas.models.responses.ApiResponses as ResponsesModel
import io.swagger.v3.oas.models.tags.Tag as TagModel
import swagger.grails4.SchemaType
import swagger.grails4.model.TypeAndFormat

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter

@Slf4j
class GrailsReader implements OpenApiReader {

    private OpenAPIConfiguration openAPIConfiguration
    private DefaultGrailsApplication grailsApplication
    private OpenAPI openAPI = new OpenAPI()
    UrlMappingsHolder urlMappingsHolder

    GrailsReader(DefaultGrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        this.urlMappingsHolder = grailsApplication.mainContext.getBean('grailsUrlMappingsHolder', UrlMappingsHolder)
    }

    @Override
    void setConfiguration(OpenAPIConfiguration openApiConfiguration) {
        this.openAPIConfiguration = openAPIConfiguration
        this.openAPI = openApiConfiguration.openAPI ? openApiConfiguration.openAPI : this.openAPI
    }

    @Override
    OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
        List<GrailsControllerClass> controllers = grailsApplication.getArtefacts('Controller')
        classes.each { Class controllerClass ->
            log.info("Processing controller: ${controllerClass}")
            GrailsControllerClass controllerArtifact = controllers.find { it.clazz == controllerClass }
            TagModel tag = buildTagModel(controllerArtifact)
            openAPI.addTagsItem(tag)
            openAPI.setPaths(openAPI.paths ?: new PathsModel()) // paths is initialized as null

            controllerArtifact.actions.each { String actionName ->
                log.info("Processing action: ${actionName}")
                Method method = findControllerMethodFromAction(controllerClass, actionName)
                OperationModel operation = buildOperationModel(controllerArtifact, method)
                if (operation) {
                    operation.addTagsItem(tag.name)
                    buildAndAddPathItemModel(controllerArtifact, method, operation)
                }
            }
        }
        return openAPI
    }

    private TagModel buildTagModel(GrailsControllerClass controller) {
        TagModel tagModel = new TagModel()
        TagAnnotation tagAnnotation = controller.clazz.getAnnotation(TagAnnotation) as TagAnnotation
        tagModel.name(tagAnnotation.name())
        tagModel.description(tagAnnotation.description())
    }

    private void buildAndAddPathItemModel(GrailsControllerClass controller, Method method, OperationModel operation) {
        PathItemModel pathItemModel = new PathItemModel()
        PathItemModel.HttpMethod httpMethod = PathItemModel.HttpMethod.GET
        String url = ''
        UrlMapping urlMapping = getUrlMappingOfAction(controller, method.name)
        if (urlMapping) {
            // TODO can we handle UrlMappings magic?
            url = urlMapping.urlData.urlPattern
            String httpMethodStr = urlMapping.httpMethod.toUpperCase()
            httpMethod = PathItemModel.HttpMethod.valueOf(httpMethodStr)
        } else {
            // UrlMapping is not explicitly defined for controller/action, so we have to build it from the controller
            UrlCreator urlCreator = urlMappingsHolder.getReverseMapping(
                    controller.logicalPropertyName, method.name, controller.pluginName, [:])
            url = urlCreator.createURL(controller.logicalPropertyName, method.name, [:], 'UTF-8')
        }
        pathItemModel.operation(httpMethod, operation)
        openAPI.paths.addPathItem(url, pathItemModel)
    }

    private OperationModel buildOperationModel(GrailsControllerClass controller, Method method) {
        OperationAnnotation operationAnnotation = method.getAnnotation(OperationAnnotation)
        if (operationAnnotation) {
            OperationModel operationModel = new OperationModel()

            Paranamer paranamer = new CachingParanamer(new BytecodeReadingParanamer())
            List<String> paramNames = paranamer.lookupParameterNames(method)

            // Build parameters of operation
            method.parameters.eachWithIndex { Parameter parameter, int i ->
                String parameterName = paramNames[i]
                ParameterModel parameterModel = buildParameterModel(operationAnnotation, parameter, parameterName)
                operationModel.addParametersItem(parameterModel)
            }

            // Build responses of operation
            operationAnnotation.responses().each { ResponseAnnotation apiResponse ->
                ResponsesModel responsesModel = buildResponsesModel(operationAnnotation)
                operationModel.setResponses(responsesModel)
            }

            return operationModel
        }
        return null
    }

    private ParameterModel buildParameterModel(OperationAnnotation operationAnnotation, Parameter parameter, String paramName) {
        ParameterAnnotation parameterAnnotation = operationAnnotation.parameters().find { it.name() == paramName }
        ParameterModel parameterModel = new ParameterModel()
        // TODO $ref
        parameterModel.setName(parameterAnnotation.name())
        parameterModel.setDescription(parameterAnnotation.description())
        parameterModel.setIn(parameterAnnotation.in()?.toString())
        parameterModel.setRequired(parameterAnnotation.required())
        parameterModel.setDeprecated(parameterAnnotation.deprecated())
        parameterModel.setAllowEmptyValue(parameterAnnotation.allowEmptyValue())
        parameterModel.setSchema(buildSchemaModel(parameterAnnotation.schema()))
//        ParameterModel parameterModel = SwaggerAnnotationMapper.mapParameterAnnotation(parameterAnnotation)
        if (parameterAnnotation.schema()) {

        }
        return parameterModel
    }

    private ResponsesModel buildResponsesModel(OperationAnnotation operationAnnotation) {
        ResponsesModel responsesModel = new ResponsesModel()
        operationAnnotation.responses().each { ResponseAnnotation responseAnnotation ->
            ResponseModel responseModel = new ResponseModel()
            // TODO map $ref, content and extensions
            responseModel.setDescription(responseAnnotation.description())
            responseModel.setHeaders(SwaggerAnnotationMapper.mapHeadersAnnotation(responseAnnotation.headers()))
            responseModel.setLinks(SwaggerAnnotationMapper.mapLinksAnnotation(responseAnnotation.links()))
            responseModel.setContent(buildContentModel(responseAnnotation.content()))

            responsesModel.addApiResponse(responseAnnotation.responseCode(), responseModel)
        }
        return responsesModel
    }

    private ContentModel buildContentModel(ContentAnnotation[] contentAnnotations) {
        ContentModel contentModel = new ContentModel()
        contentAnnotations.each { ContentAnnotation contentAnnotation ->
            contentModel.addMediaType(contentAnnotation.mediaType(), buildMediaTypeModel(contentAnnotation.schema()))
        }
    }

    private MediaTypeModel buildMediaTypeModel(SchemaAnnotation schemaAnnotation) {
        MediaTypeModel mediaTypeModel = new MediaTypeModel()
        // TODO examples, encoding, extensions
        mediaTypeModel.setSchema(buildSchemaModel(schemaAnnotation))
        return mediaTypeModel
    }

    private UrlMapping getUrlMappingOfAction(GrailsControllerClass controller, String actionName) {
        return urlMappingsHolder.urlMappings.find {
            it.controllerName == controller.logicalPropertyName && it.actionName == actionName
        }
    }

    private SchemaModel buildSchemaModel(SchemaAnnotation schemaAnnotation) {
        if (schemaAnnotation?.implementation() && schemaAnnotation?.implementation() != Void) {
            return buildSchemaModel(schemaAnnotation?.implementation())
        } else {
            return new SchemaModel(type: schemaAnnotation.type(), format: schemaAnnotation.format())
        }
    }

    private SchemaModel buildSchemaModel(Class schemaClass) {
        SchemaModel existingSchema = findSchemaModelInOpenAPI(schemaClass)
        if (existingSchema) {
            return new SchemaModel($ref: getSchemaRef(existingSchema))
        }
        // Schema does not already exist, so we build it
        SchemaModel schemaModel = new SchemaModel()
        TypeAndFormat typeAndFormat = findTypeAndFormat(schemaClass)
        schemaModel.setType(typeAndFormat.type.name)
        schemaModel.setFormat(typeAndFormat.format)

        // is the type is an object, we build the schema from its properties
        if (typeAndFormat.type == SchemaType.OBJECT || schemaClass.isEnum()) {
            Map<String, SchemaModel> schemaProperties = buildSchemaProperties(schemaClass)
            schemaModel.properties(schemaProperties)
            openAPI.schema(schemaNameFromClass(schemaClass), schemaModel)
        }
        return schemaModel
    }

    private Map<String, SchemaModel> buildSchemaProperties(Class clazz) {
        SortedMap<String, SchemaModel> propMap = new TreeMap<>()
        clazz.metaClass.properties.each { MetaProperty prop ->
            if (!(prop.modifiers & Modifier.PUBLIC)) {
                return
            }

            String fieldName = prop.name
            Class fieldType = prop.type

            // Try to find schema for the property type
            SchemaModel propSchema = findSchemaModelInOpenAPI(fieldType)
            if (!propSchema) {
                propSchema = buildSchemaModel(fieldType)
            }
            propMap[fieldName] = propSchema
        }
    }

    private SchemaModel findSchemaModelInOpenAPI(Class clazz) {
        String className = schemaNameFromClass(clazz)
        return openAPI.components?.getSchemas()?.get(className)
    }

    private static String schemaNameFromClass(Class clazz) {
        return clazz.canonicalName
    }

    @CompileStatic
    private static TypeAndFormat findTypeAndFormat(Class schemaClass, SchemaAnnotation schemaAnnotation = null) {
        if (schemaAnnotation?.type()) {
            // If the user has supplied type and format via annotation, use that
            return new TypeAndFormat(SchemaType.fromSwaggerName(schemaAnnotation.type()), schemaAnnotation.format())
        } else {
            // Else we find it dynamically based on the implementation parameter
            return TypeAndFormat.fromClass(schemaClass)
        }
    }

    /**
     * A class' methods are listed multiple times, with different numbers of parameters.
     * This function finds the class method with the most parameters ("all" parameters).
     */
    @CompileStatic
    private static findControllerMethodFromAction(Class controllerClass, String actionName) {
        return controllerClass.methods
                .findAll { it.name == actionName }
                .sort { it.parameterCount }
                .last()
    }

    @CompileStatic
    static String getSchemaRef(SchemaModel schema) {
        "#/components/schemas/${schema.name}"
    }
}
