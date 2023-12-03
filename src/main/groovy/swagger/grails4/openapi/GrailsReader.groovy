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
import io.swagger.v3.oas.annotations.ExternalDocumentation as ExternalDocumentationAnnotation
import io.swagger.v3.oas.annotations.Operation as OperationAnnotation
import io.swagger.v3.oas.annotations.Parameter as ParameterAnnotation
import io.swagger.v3.oas.annotations.media.Content as ContentAnnotation
import io.swagger.v3.oas.annotations.media.ExampleObject as ExampleAnnotation
import io.swagger.v3.oas.annotations.media.Schema as SchemaAnnotation
import io.swagger.v3.oas.annotations.parameters.RequestBody as RequestBodyAnnotation
import io.swagger.v3.oas.annotations.responses.ApiResponse as ResponseAnnotation
import io.swagger.v3.oas.annotations.security.SecurityRequirement as SecurityRequirementAnnotation
import io.swagger.v3.oas.annotations.servers.Server as ServerAnnotation
import io.swagger.v3.oas.annotations.servers.ServerVariable as ServerVariableAnnotation
import io.swagger.v3.oas.annotations.tags.Tag as TagAnnotation
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration
import io.swagger.v3.oas.integration.api.OpenApiReader
import io.swagger.v3.oas.models.ExternalDocumentation as ExternalDocumentationModel
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation as OperationModel
import io.swagger.v3.oas.models.PathItem as PathItemModel
import io.swagger.v3.oas.models.Paths as PathsModel
import io.swagger.v3.oas.models.examples.Example as ExampleModel
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Content as ContentModel
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.MediaType as MediaTypeModel
import io.swagger.v3.oas.models.media.Schema as SchemaModel
import io.swagger.v3.oas.models.parameters.Parameter as ParameterModel
import io.swagger.v3.oas.models.parameters.RequestBody as RequestBodyModel
import io.swagger.v3.oas.models.responses.ApiResponse as ResponseModel
import io.swagger.v3.oas.models.responses.ApiResponses as ResponsesModel
import io.swagger.v3.oas.models.security.SecurityRequirement as SecurityRequirementModel
import io.swagger.v3.oas.models.servers.Server as ServerModel
import io.swagger.v3.oas.models.servers.ServerVariable as ServerVariableModel
import io.swagger.v3.oas.models.servers.ServerVariables as ServerVariablesModel
import io.swagger.v3.oas.models.tags.Tag as TagModel
import swagger.grails4.enums.SchemaType
import swagger.grails4.helpers.EnumMapper
import swagger.grails4.helpers.GroovyClassHelper
import swagger.grails4.model.TypeAndFormat

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import java.lang.reflect.Type

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
            openAPI.setPaths(openAPI.paths ?: new PathsModel()) // paths is initialized as null in OpenAPI

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
        tagModel.setName(tagAnnotation.name())
        tagModel.setDescription(tagAnnotation.description())

        // TODO support extensions
        ExternalDocumentationModel externalDocumentationModel = buildExternalDocumentationModel(tagAnnotation.externalDocs())
        tagModel.setExternalDocs(externalDocumentationModel)
        return tagModel
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
            // TODO support callbacks
            OperationModel operationModel = new OperationModel()
            operationModel.setSummary(operationAnnotation.summary())
            operationModel.setDescription(operationAnnotation.description())
            operationModel.setExternalDocs(buildExternalDocumentationModel(operationAnnotation.externalDocs()))
            operationModel.setOperationId(operationAnnotation.operationId())
            operationModel.setRequestBody(buildRequestBodyModel(operationAnnotation.requestBody()))
            operationModel.setDeprecated(operationAnnotation.deprecated())
            operationModel.setSecurity(buildSecurityRequirementModels(operationAnnotation.security()))
            operationModel.setServers(buildServerModels(operationAnnotation.servers()))

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

    private RequestBodyModel buildRequestBodyModel(RequestBodyAnnotation requestBodyAnnotation) {
        RequestBodyModel requestBodyModel = new RequestBodyModel()
        requestBodyModel.setDescription(requestBodyAnnotation?.description())
        requestBodyModel.setContent(buildContentModel(requestBodyAnnotation?.content()))
        requestBodyModel.setRequired(requestBodyAnnotation?.required())
        return requestBodyModel
    }

    private ParameterModel buildParameterModel(OperationAnnotation operationAnnotation, Parameter parameter, String paramName) {
        ParameterAnnotation parameterAnnotation = operationAnnotation.parameters().find { it.name() == paramName }
        ParameterModel parameterModel = new ParameterModel()
        parameterModel.setName(parameterAnnotation?.name() ?: paramName)
        parameterModel.setIn(parameterAnnotation?.in()?.toString())
        parameterModel.setDescription(parameterAnnotation?.description())
        parameterModel.setRequired(parameterAnnotation?.required())
        parameterModel.setDeprecated(parameterAnnotation?.deprecated())
        parameterModel.setAllowEmptyValue(parameterAnnotation?.allowEmptyValue())
        parameterModel.setStyle(EnumMapper.styleEnumFromParameterStyle(parameterAnnotation?.style()))
        parameterModel.setExplode(EnumMapper.explodeToBoolean(parameterAnnotation?.explode()))
        parameterModel.setAllowReserved(parameterAnnotation?.allowReserved())
        parameterModel.setExamples(buildExampleModels(parameterAnnotation?.examples()))
        parameterModel.setExample(parameterAnnotation?.example() ?: null)
        parameterModel.setSchema(buildSchemaModel(parameterAnnotation?.schema(), parameter.type))
        parameterModel.setContent(buildContentModel(parameterAnnotation?.content()))
        return parameterModel
    }

    private Map<String, ExampleModel> buildExampleModels(ExampleAnnotation[] exampleAnnotations) {
        Map<String, ExampleModel> exampleMap = new HashMap<>()
        exampleAnnotations?.each { ExampleAnnotation exampleAnnotation ->
            ExampleModel exampleModel = new ExampleModel()
            exampleModel.setSummary(exampleAnnotation.summary())
            exampleModel.setDescription(exampleAnnotation.description())
            exampleModel.setValue(exampleAnnotation.value())
            exampleModel.setExternalValue(exampleAnnotation.externalValue())
            exampleMap.put(exampleAnnotation.name(), exampleModel)
        }
        return exampleMap.isEmpty() ? null : exampleMap // If empty map is returned, an empty list of examples is shown in ui
    }

    private ResponsesModel buildResponsesModel(OperationAnnotation operationAnnotation) {
        ResponsesModel responsesModel = new ResponsesModel()
        operationAnnotation.responses().each { ResponseAnnotation responseAnnotation ->
            ResponseModel responseModel = new ResponseModel()
            responseModel.setDescription(responseAnnotation.description())
            responseModel.setHeaders(SwaggerAnnotationMapper.mapHeadersAnnotation(responseAnnotation.headers()))
            responseModel.setLinks(SwaggerAnnotationMapper.mapLinksAnnotation(responseAnnotation.links()))
            responseModel.setContent(buildContentModel(responseAnnotation.content()))

            responsesModel.addApiResponse(responseAnnotation.responseCode(), responseModel)
        }
        return responsesModel
    }

    private ExternalDocumentationModel buildExternalDocumentationModel(ExternalDocumentationAnnotation externalDocAnnotation) {
        ExternalDocumentationModel externalDocumentationModel = new ExternalDocumentationModel()
        externalDocumentationModel.setDescription(externalDocAnnotation?.description())
        externalDocumentationModel.setUrl(externalDocAnnotation?.url())
        return externalDocumentationModel
    }

    private List<SecurityRequirementModel> buildSecurityRequirementModels(SecurityRequirementAnnotation[] securityRequirementAnnotations) {
        return securityRequirementAnnotations?.collect { SecurityRequirementAnnotation securityRequirementAnnotation ->
            SecurityRequirementModel securityRequirementModel = new SecurityRequirementModel()
            securityRequirementModel.addList(securityRequirementAnnotation.name(), securityRequirementAnnotation.scopes()?.toList())
            return securityRequirementModel
        }
    }

    private List<ServerModel> buildServerModels(ServerAnnotation[] serverAnnotations) {
        return serverAnnotations?.collect { ServerAnnotation serverAnnotation ->
            ServerModel serverModel = new ServerModel()
            serverModel.setUrl(serverAnnotation.url())
            serverModel.setDescription(serverAnnotation.description())
            serverModel.setVariables(buildServerVariablesModel(serverAnnotation.variables()))
        }
    }

    private ServerVariablesModel buildServerVariablesModel(ServerVariableAnnotation[] serverVariableAnnotations) {
        ServerVariablesModel serverVariablesModel = new ServerVariablesModel()
        serverVariableAnnotations?.each { ServerVariableAnnotation serverVariableAnnotation ->
            ServerVariableModel serverVariableModel = new ServerVariableModel()
            serverVariableModel.setEnum(serverVariableAnnotation.allowableValues()?.toList())
            serverVariableModel.setDefault(serverVariableAnnotation.defaultValue())
            serverVariableModel.setDescription(serverVariableAnnotation.description())
            serverVariablesModel.addServerVariable(serverVariableAnnotation.name(), serverVariableModel)
        }
        return serverVariablesModel
    }

    private ContentModel buildContentModel(ContentAnnotation[] contentAnnotations) {
        ContentModel contentModel = new ContentModel()
        contentAnnotations?.each { ContentAnnotation contentAnnotation ->
            contentModel.addMediaType(contentAnnotation.mediaType(), buildMediaTypeModel(contentAnnotation.schema()))
        }
        return contentModel
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
        return buildSchemaModel(schemaAnnotation, schemaAnnotation?.implementation())
    }

    private SchemaModel buildSchemaModel(Class schemaClass) {
        return buildSchemaModel(null, schemaClass)
    }

    private SchemaModel buildSchemaModel(Class schemaClass, Type genericType) {
        return buildSchemaModel(null, schemaClass, genericType)
    }

    private SchemaModel buildSchemaModel(SchemaAnnotation schemaAnnotation, Class schemaClass, Type genericType = null) {
        SchemaModel existingSchema = schemaClass ? findSchemaModelInOpenAPI(schemaClass) : null
        if (existingSchema) {
            return new SchemaModel($ref: getSchemaRef(existingSchema))
        }
        // Schema does not already exist, so we build it. Annotation takes precedence
        // TODO support more properties from SchemaAnnotation
        Map schemaArgs = buildSchemaModelArgs(schemaAnnotation, schemaClass)
        SchemaModel schemaModel = new SchemaModel(schemaArgs)
        String name = schemaModel.name

        if (schemaModel.type == SchemaType.OBJECT.swaggerName) { // Type is object
            if (Map.isAssignableFrom(schemaClass)) { // Object type is Map
                schemaModel = new MapSchema(schemaArgs)
                Class componentClass = schemaClass.componentType ?: (genericType?.actualTypeArguments[0] as Class)
                componentClass = componentClass ?: Object
                schemaModel.additionalProperties = buildSchemaModel(componentClass)
            } else { // All other types of objects
                Map<String, SchemaModel> schemaProperties = buildSchemaProperties(schemaClass)
                schemaModel.properties(schemaProperties)
                openAPI.schema(name, schemaModel)
            }
        } else if (schemaModel.type == SchemaType.ARRAY.swaggerName) { // Type if List-like collection
            schemaModel = new ArraySchema(schemaArgs)
            Class componentClass = schemaClass.componentType ?: (genericType?.actualTypeArguments[0] as Class)
            componentClass = componentClass ?: Object
            schemaModel.items = buildSchemaModel(componentClass)
        } else if (schemaClass.isEnum()) { // Type is enum
            schemaModel.enum = schemaModel.enum ?: schemaClass.values().collect { it.name() }
            openAPI.schema(name, schemaModel) // Enums are also saved as "reusable enums"
        }
        return schemaModel
    }

    private Map<String, Object> buildSchemaModelArgs(SchemaAnnotation schemaAnnotation, Class schemaClass) {
        TypeAndFormat typeAndFormat = findTypeAndFormat(schemaClass)
        String type = schemaAnnotation?.type() ?: typeAndFormat.typeName
        String format = schemaAnnotation?.format() ?: typeAndFormat.format
        String name = schemaNameFromClass(schemaClass)
        return [
            type       : type,
            format     : format,
            name       : name,
            title      : schemaAnnotation?.title(),
            description: schemaAnnotation?.description(),
            enum       : schemaAnnotation?.allowableValues()
        ]
    }

    private Map<String, SchemaModel> buildSchemaProperties(Class clazz) {
        SortedMap<String, SchemaModel> propMap = new TreeMap<>()
        clazz.metaClass.properties.each { MetaProperty prop ->
            if (!(prop.modifiers & Modifier.PUBLIC)) {
                return
            }

            String fieldName = prop.name
            Class fieldType = prop.type

            if (GroovyClassHelper.isGroovyProperty(clazz, prop)) {
                return
            }

            // Try to find schema for the property type
            SchemaModel propSchema = findSchemaModelInOpenAPI(fieldType)
            if (!propSchema) {
                SchemaAnnotation schemaAnnotation = prop.field?.field?.getAnnotation(SchemaAnnotation)
                Type genericType = prop.field?.field?.genericType // Used to find out component class of Collections
                propSchema = buildSchemaModel(schemaAnnotation, fieldType, genericType)
            }
            propMap[fieldName] = propSchema
        }
        return propMap
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
        return "#/components/schemas/${schema.name}"
    }
}
