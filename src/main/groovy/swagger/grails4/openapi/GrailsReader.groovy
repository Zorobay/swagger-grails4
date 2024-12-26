package swagger.grails4.openapi

import com.thoughtworks.paranamer.BytecodeReadingParanamer
import com.thoughtworks.paranamer.CachingParanamer
import com.thoughtworks.paranamer.Paranamer
import grails.core.DefaultGrailsApplication
import grails.core.GrailsControllerClass
import grails.util.Holders
import grails.web.mapping.UrlCreator
import grails.web.mapping.UrlMapping
import grails.web.mapping.UrlMappingsHolder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.swagger.v3.oas.annotations.ExternalDocumentation as ExternalDocumentationAnnotation
import io.swagger.v3.oas.annotations.Operation as OperationAnnotation
import io.swagger.v3.oas.annotations.Parameter as ParameterAnnotation
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.headers.Header as HeaderAnnotation
import io.swagger.v3.oas.annotations.links.Link as LinkAnnotation
import io.swagger.v3.oas.annotations.links.LinkParameter as LinkParameterAnnotation
import io.swagger.v3.oas.annotations.media.ArraySchema as ArraySchemaAnnotation
import io.swagger.v3.oas.annotations.media.Content as ContentAnnotation
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping as DiscriminatorMappingAnnotation
import io.swagger.v3.oas.annotations.media.Encoding as EncodingAnnotation
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
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.links.Link
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Discriminator
import io.swagger.v3.oas.models.media.Encoding
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.servers.ServerVariable
import io.swagger.v3.oas.models.servers.ServerVariables
import io.swagger.v3.oas.models.tags.Tag
import org.grails.config.NavigableMap
import swagger.grails4.enums.SchemaType
import swagger.grails4.helpers.EnumMapper
import swagger.grails4.helpers.GroovyClassHelper
import swagger.grails4.helpers.MapHelper
import swagger.grails4.helpers.ValueMapper
import swagger.grails4.model.TypeAndFormat

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter as JavaParameter
import java.lang.reflect.Type

@Slf4j
class GrailsReader implements OpenApiReader {

    private DefaultGrailsApplication grailsApplication
    private OpenAPI openAPI
    UrlMappingsHolder urlMappingsHolder

    GrailsReader(DefaultGrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        this.urlMappingsHolder = grailsApplication.mainContext.getBean('grailsUrlMappingsHolder', UrlMappingsHolder)
    }

    @Override
    void setConfiguration(OpenAPIConfiguration ignored) { /* This is never called?*/ }

    @Override
    OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
        openAPI = buildOpenAPI()
        List<GrailsControllerClass> controllers = grailsApplication.getArtefacts('Controller')
        classes.each { Class controllerClass ->
            log.info("Processing controller: ${controllerClass}")
            GrailsControllerClass controllerArtifact = controllers.find { it.clazz == controllerClass }
            Tag tag = buildTag(controllerArtifact)
            openAPI.addTagsItem(tag)
            openAPI.setPaths(openAPI.paths ?: new Paths()) // paths is initialized as null in OpenAPI

            controllerArtifact.actions.each { String actionName ->
                log.info("Processing action: ${actionName}")
                Method method = findControllerMethodFromAction(controllerClass, actionName)
                Operation operation = buildOperation(controllerArtifact, method)
                if (operation) {
                    operation.tags ?: operation.addTagsItem(tag.name) // Add default tag if none are specified with @Operation(tags=[])
                    buildAndAddPathItem(controllerArtifact, method, operation)
                }
            }
        }
        return openAPI
    }

    private Tag buildTag(GrailsControllerClass controller) {
        Tag tag = new Tag()
        TagAnnotation tagAnnotation = controller.clazz.getAnnotation(TagAnnotation) as TagAnnotation
        tag.setName(tagAnnotation.name())
        tag.setDescription(tagAnnotation.description())

        // TODO support extensions
        ExternalDocumentation externalDocumentation = buildExternalDocumentation(tagAnnotation.externalDocs())
        tag.setExternalDocs(externalDocumentation)
        return tag
    }

    private void buildAndAddPathItem(GrailsControllerClass controller, Method method, Operation operation) {
        PathItem pathItem = new PathItem()
        PathItem.HttpMethod httpMethod = PathItem.HttpMethod.GET
        String url
        UrlMapping urlMapping = getUrlMappingOfAction(controller, method.name)
        if (urlMapping) {
            // TODO can we handle UrlMappings magic?
            url = urlMapping.urlData.urlPattern
            String httpMethodStr = urlMapping.httpMethod.toUpperCase()
            httpMethod = PathItem.HttpMethod.valueOf(httpMethodStr)
        } else {
            // UrlMapping is not explicitly defined for controller/action, so we have to build it from the controller
            UrlCreator urlCreator = urlMappingsHolder.getReverseMapping(
                controller.logicalPropertyName, method.name, controller.pluginName, [:])
            url = urlCreator.createURL(controller.logicalPropertyName, method.name, [:], 'UTF-8')
        }
        pathItem.operation(httpMethod, operation)
        openAPI.paths.addPathItem(url, pathItem)
    }

    private Operation buildOperation(GrailsControllerClass controller, Method method) {
        OperationAnnotation operationAnnotation = method.getAnnotation(OperationAnnotation)
        if (operationAnnotation) {
            // TODO support callbacks
            Operation operation = new Operation()
            operation.setTags(operationAnnotation.tags() as List<String>)
            operation.setSummary(operationAnnotation.summary())
            operation.setDescription(operationAnnotation.description())
            operation.setExternalDocs(buildExternalDocumentation(operationAnnotation.externalDocs()))
            operation.setOperationId(operationAnnotation.operationId())
            operation.setRequestBody(buildRequestBody(operationAnnotation.requestBody()))
            operation.setDeprecated(operationAnnotation.deprecated())
            operation.setSecurity(buildSecurityRequirements(operationAnnotation.security()))
            operation.setServers(buildServers(operationAnnotation.servers()))

            Paranamer paranamer = new CachingParanamer(new BytecodeReadingParanamer())
            List<String> paramNames = paranamer.lookupParameterNames(method)

            // Build parameters of operation
            // Check if there is a single command object as parameter
            if (method.parameterCount == 1 && typeIsCommandObject(method.parameters.first().type)) {
                ParameterAnnotation parameterAnnotation = operationAnnotation.parameters()
                    .find { it.name() == paramNames[0] }
                operation.setParameters(buildParametersFromCommand(parameterAnnotation, method.parameters.first().type))
            } else {
                method.parameters.eachWithIndex { JavaParameter javaParam, int i ->
                    String parameterName = paramNames[i]
                    Parameter parameter = buildParameter(operationAnnotation, javaParam, parameterName)
                    operation.addParametersItem(parameter)
                }
            }

            // Build responses of operation
            operationAnnotation.responses().each { ResponseAnnotation apiResponse ->
                ApiResponses responses = buildResponses(operationAnnotation)
                operation.setResponses(responses)
            }

            return operation
        }
        return null
    }

    private RequestBody buildRequestBody(RequestBodyAnnotation requestBodyAnnotation) {
        RequestBody requestBody = new RequestBody()
        requestBody.setDescription(requestBodyAnnotation?.description())
        requestBody.setContent(buildContent(requestBodyAnnotation?.content()))
        requestBody.setRequired(requestBodyAnnotation?.required())
        return requestBody
    }

    private Parameter buildParameter(OperationAnnotation operationAnnotation, JavaParameter javaParam, String paramName) {
        ParameterAnnotation parameterAnnotation = operationAnnotation.parameters().find { it.name() == paramName }
        Parameter parameter = new Parameter()
        parameter.setName(parameterAnnotation?.name() ?: paramName)
        parameter.setIn(parameterAnnotation?.in()?.toString())
        parameter.setDescription(parameterAnnotation?.description())
        parameter.setRequired(parameterAnnotation?.required())
        parameter.setDeprecated(parameterAnnotation?.deprecated())
        parameter.setAllowEmptyValue(parameterAnnotation?.allowEmptyValue())
        parameter.setStyle(EnumMapper.styleEnumFromParameterStyle(parameterAnnotation?.style()))
        parameter.setExplode(EnumMapper.explodeToBoolean(parameterAnnotation?.explode()))
        parameter.setAllowReserved(parameterAnnotation?.allowReserved())
        parameter.setExamples(buildExamples(parameterAnnotation?.examples()))
        parameter.setExample(parameterAnnotation?.example() ?: null)
        parameter.setSchema(buildSchema(parameterAnnotation?.schema(), javaParam.type))
        parameter.setContent(buildContent(parameterAnnotation?.content()))
        return parameter
    }

    private List<Parameter> buildParametersFromCommand(ParameterAnnotation parameterAnnotation, Class commandClass) {
        ParameterIn inType = parameterAnnotation.in()
        Map<String, Schema> properties = buildSchemaProperties(commandClass)

        return properties.collect { String key, Schema val ->
            Parameter parameter = new Parameter()
            parameter.setName(key)
            parameter.setDescription(val?.getDescription())
            parameter.setExample(val?.example)
            parameter.setIn(inType?.toString())
            parameter.setSchema(val)
            return parameter
        }
    }

    private Map<String, Example> buildExamples(ExampleAnnotation[] exampleAnnotations) {
        Map<String, Example> exampleMap = new HashMap<>()
        exampleAnnotations?.each { ExampleAnnotation exampleAnnotation ->
            Example example = new Example()
            example.setSummary(exampleAnnotation.summary())
            example.setDescription(exampleAnnotation.description())
            example.setValue(exampleAnnotation.value())
            example.setExternalValue(exampleAnnotation.externalValue())
            exampleMap.put(exampleAnnotation.name(), example)
        }

        // If empty map is returned, an empty list of examples is shown in ui, therefore we return null
        return exampleMap.isEmpty() ? null : exampleMap
    }

    private ApiResponses buildResponses(OperationAnnotation operationAnnotation) {
        ApiResponses responses = new ApiResponses()
        operationAnnotation.responses().each { ResponseAnnotation responseAnnotation ->
            ApiResponse response = new ApiResponse()
            response.setDescription(responseAnnotation.description())
            responseAnnotation.headers().each { HeaderAnnotation headerAnnotation ->
                response.addHeaderObject(headerAnnotation.name(), buildHeader(headerAnnotation))
            }
            responseAnnotation.links().each { LinkAnnotation linkAnnotation ->
                response.addLink(linkAnnotation.name(), buildLink(linkAnnotation))
            }
            response.setContent(buildContent(responseAnnotation.content()))
            responses.addApiResponse(responseAnnotation.responseCode(), response)
        }
        return responses
    }

    private Link buildLink(LinkAnnotation linkAnnotation) {
        Link link = new Link()
        link.setOperationRef(linkAnnotation.operationRef())
        link.setOperationId(linkAnnotation.operationId())
        linkAnnotation.parameters().each { LinkParameterAnnotation linkParameterAnnotation ->
            link.addParameter(linkParameterAnnotation.name(), linkParameterAnnotation.expression())
        }
        link.setRequestBody(linkAnnotation.requestBody())
        return link
    }

    private ExternalDocumentation buildExternalDocumentation(ExternalDocumentationAnnotation externalDocAnnotation) {
        ExternalDocumentation externalDocumentation = new ExternalDocumentation()
        externalDocumentation.setDescription(externalDocAnnotation?.description())
        externalDocumentation.setUrl(externalDocAnnotation?.url())
        return externalDocumentation
    }

    private List<SecurityRequirement> buildSecurityRequirements(SecurityRequirementAnnotation[] securityRequirementAnnotations) {
        return securityRequirementAnnotations?.collect { SecurityRequirementAnnotation securityRequirementAnnotation ->
            SecurityRequirement securityRequirement = new SecurityRequirement()
            securityRequirement.addList(securityRequirementAnnotation.name(), securityRequirementAnnotation.scopes()?.toList())
            return securityRequirement
        }
    }

    private List<Server> buildServers(ServerAnnotation[] serverAnnotations) {
        return serverAnnotations?.collect { ServerAnnotation serverAnnotation ->
            Server server = new Server()
            server.setUrl(serverAnnotation.url())
            server.setDescription(serverAnnotation.description())
            server.setVariables(buildServerVariables(serverAnnotation.variables()))
        }
    }

    private ServerVariables buildServerVariables(ServerVariableAnnotation[] serverVariableAnnotations) {
        ServerVariables serverVariables = new ServerVariables()
        serverVariableAnnotations?.each { ServerVariableAnnotation serverVariableAnnotation ->
            ServerVariable serverVariable = new ServerVariable()
            serverVariable.setEnum(serverVariableAnnotation.allowableValues()?.toList())
            serverVariable.setDefault(serverVariableAnnotation.defaultValue())
            serverVariable.setDescription(serverVariableAnnotation.description())
            serverVariables.addServerVariable(serverVariableAnnotation.name(), serverVariable)
        }
        return serverVariables
    }

    private Content buildContent(ContentAnnotation[] contentAnnotations) {
        Content content = new Content()
        contentAnnotations?.each { ContentAnnotation contentAnnotation ->
            MediaType mediaType = new MediaType()
            if (ContentAnnotation.class.getMethod('schema').getDefaultValue() == contentAnnotation.schema()) {
                mediaType = buildMediaType(contentAnnotation.array())
            } else {
                mediaType = buildMediaType(contentAnnotation.schema())
            }
            // Build encoding model for each MediaType from the ContentAnnotation
            contentAnnotation.encoding()?.each { EncodingAnnotation encodingAnnotation ->
                mediaType.addEncoding(encodingAnnotation.name(), buildEncoding(encodingAnnotation))
            }

            content.addMediaType(contentAnnotation.mediaType(), mediaType)
        }
        return content
    }

    private MediaType buildMediaType(SchemaAnnotation schemaAnnotation) {
        MediaType mediaType = new MediaType()
        mediaType.setSchema(buildSchema(schemaAnnotation))
        if (schemaAnnotation.example()) {
            mediaType.setExample(schemaAnnotation.example())
        }
        return mediaType
    }

    private MediaType buildMediaType(ArraySchemaAnnotation arraySchemaAnnotation) {
        MediaType mediaType = new MediaType()
        ArraySchema arraySchema = buildArraySchema(arraySchemaAnnotation)
        arraySchema.setMaxItems(arraySchemaAnnotation.maxItems())
        arraySchema.setMinItems(arraySchemaAnnotation.minItems())
        mediaType.setSchema(buildArraySchema(arraySchemaAnnotation))
        return mediaType
    }

    private Encoding buildEncoding(EncodingAnnotation encodingAnnotation) {
        Encoding encoding = new Encoding()
        encoding.setContentType(encodingAnnotation.contentType())
        encodingAnnotation.headers().each { HeaderAnnotation headerAnnotation ->
            encoding.addHeader(headerAnnotation.name(), buildHeader(headerAnnotation))
        }
        // TODO not sure how styles are used here https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#encodingStyle
        // encoding.setStyle(??)
        encoding.setExplode(encodingAnnotation.explode())
        encoding.setAllowReserved(encodingAnnotation.allowReserved())
        return encoding
    }

    private Header buildHeader(HeaderAnnotation headerAnnotation) {
        Header header = new Header()
        header.setDescription(headerAnnotation.description())
        header.setRequired(headerAnnotation.required())
        header.setDeprecated(headerAnnotation.deprecated())
        header.setSchema(buildSchema(headerAnnotation.schema()))
        return header
    }

    private UrlMapping getUrlMappingOfAction(GrailsControllerClass controller, String actionName) {
        return urlMappingsHolder.urlMappings.find {
            it.controllerName == controller.logicalPropertyName && it.actionName == actionName
        }
    }

    private ArraySchema buildArraySchema(ArraySchemaAnnotation arraySchemaAnnotation) {
        if (arraySchemaAnnotation) {
            ArraySchema arraySchema = new ArraySchema()
            arraySchema.items(buildSchema(arraySchemaAnnotation.items(), arraySchemaAnnotation?.items()?.implementation()))
            return arraySchema
        }
        return null
    }

    private Schema buildSchema(SchemaAnnotation schemaAnnotation) {
        if (schemaAnnotation) {
            return buildSchema(schemaAnnotation, schemaAnnotation?.implementation())
        }
        return null
    }

    private Schema buildSchema(Class schemaClass) {
        if (schemaClass && schemaClass != Void) {
            return buildSchema(null, schemaClass)
        }
        return null
    }

    private Schema buildSchema(Class schemaClass, Type genericType) {
        return buildSchema(null, schemaClass, genericType)
    }

    private Schema buildSchema(SchemaAnnotation schemaAnnotation, Class schemaClass, Type genericType = null) {
        // TODO fetch, if exists, @Schema/@ArraySchema annotation on schemaClass which overwrites
        Schema existingSchema = schemaClass ? findSchemaInOpenAPI(schemaClass) : null
        if (existingSchema) {
            return new Schema($ref: getSchemaRef(existingSchema))
        }
        // Schema does not already exist, so we build it. Annotation takes precedence
        Map schemaArgs = buildSchemaArgs(schemaAnnotation, schemaClass)

        // Fetch Schema annotation on class level, if existing. This annotation has higher precedence
        SchemaAnnotation schemaAnnotationOnClass = schemaClass?.getAnnotation(SchemaAnnotation) as SchemaAnnotation
        if (schemaAnnotationOnClass) {
            schemaArgs = MapHelper.merge(schemaArgs, buildSchemaArgs(schemaAnnotationOnClass, schemaClass))
        }
        Schema schema = new Schema(schemaArgs)
        String name = schema.name

        if (schema.type == SchemaType.OBJECT.swaggerName) { // Type is object
            if (Map.isAssignableFrom(schemaClass)) { // Object type is Map
                schema = new MapSchema(schemaArgs)
                Class componentClass = schemaClass.componentType ?: (genericType?.actualTypeArguments[0] as Class)
                componentClass = componentClass ?: Object
                schema.additionalProperties = buildSchema(componentClass)
            } else { // All other types of objects
                Map<String, Schema> schemaProperties = buildSchemaProperties(schemaClass)
                schema.properties(schemaProperties)
                openAPI.schema(name, schema)
            }
        } else if (schema.type == SchemaType.ARRAY.swaggerName) { // Type if List-like collection
            schema = new ArraySchema(schemaArgs)
            Class componentClass = schemaClass.componentType ?: (genericType?.actualTypeArguments?.getAt(0) as Class)
            componentClass = componentClass ?: Object
            schema.items = buildSchema(componentClass)
        } else if (schemaClass.isEnum()) { // Type is enum
            schema.enum = schema.enum ?: schemaClass.values().collect { it.name() }
            openAPI.schema(name, schema) // Enums are also saved as "reusable enums"
        }
        return schema
    }

    private Map<String, Object> buildSchemaArgs(SchemaAnnotation schemaAnnotation, Class schemaClass) {
        TypeAndFormat typeAndFormat = findTypeAndFormat(schemaClass)
        String type = schemaAnnotation?.type() ?: typeAndFormat.typeName
        String format = schemaAnnotation?.format() ?: typeAndFormat.format
        String name = schemaNameFromClass(schemaClass)
        Discriminator discriminator = buildDiscriminator(schemaAnnotation?.discriminatorProperty(),
            schemaAnnotation?.discriminatorMapping())
        List<Schema> prefixItems = schemaAnnotation?.prefixItems()?.collect { buildSchema(it) }
        List<Schema> allOf = schemaAnnotation?.allOf()?.collect { buildSchema(it) } ?: null
        List<Schema> anyOf = schemaAnnotation?.anyOf()?.collect { buildSchema(it) } ?: null
        List<Schema> oneOf = schemaAnnotation?.oneOf()?.collect { buildSchema(it) } ?: null
        Map<String, Schema> patternProperties = schemaAnnotation?.patternProperties()?.collectEntries {
            return [it.key(), buildSchema(it.value())]
        }

        // TODO support @ArraySchema
        Map<String, Object> args = [
            name                 : name,
            title                : schemaAnnotation?.title() ?: null,
            multipleOf           : schemaAnnotation?.multipleOf() ?: null,
            maximum              : ValueMapper.stringToBigDecimal(schemaAnnotation?.maximum()),
            exclusiveMaximum     : schemaAnnotation?.exclusiveMaximum() ?: null,
            minimum              : ValueMapper.stringToBigDecimal(schemaAnnotation?.minimum()),
            exclusiveMinimum     : schemaAnnotation?.exclusiveMinimum() ?: null,
            pattern              : schemaAnnotation?.pattern() ?: null,
            maxItems             : null, // from @ArraySchema
            minItems             : null, // from @ArraySchema
            uniqueItems          : null, // from @ArraySchema
            required             : schemaAnnotation?.requiredProperties(),
            type                 : type,
            not                  : buildSchema(schemaAnnotation?.not()),
            description          : schemaAnnotation?.description(),
            format               : format,
            nullable             : schemaAnnotation?.nullable(),
            readOnly             : EnumMapper.accessModeToReadOnly(schemaAnnotation?.accessMode()),
            writeOnly            : EnumMapper.accessModeToWriteOnly(schemaAnnotation?.accessMode()),
            externalDocs         : buildExternalDocumentation(schemaAnnotation?.externalDocs()),
            deprecated           : schemaAnnotation?.deprecated() ?: null,
            xml                  : null, // Does not exist in @Schema annotation
            enum                 : schemaAnnotation?.allowableValues(),
            discriminator        : discriminator,
            prefixItems          : prefixItems,
            allOf                : allOf,
            anyOf                : anyOf,
            oneOf                : oneOf,
            types                : schemaAnnotation?.types(),
            patternProperties    : patternProperties,
            exclusiveMaximumValue: ValueMapper.intToBigDecimal(schemaAnnotation?.exclusiveMaximumValue()),
            exclusiveMinimumValue: ValueMapper.intToBigDecimal(schemaAnnotation?.exclusiveMinimumValue()),
            contains             : buildSchema(schemaAnnotation?.contains())
        ]
        if (schemaAnnotation?.example()) {
            args.example = schemaAnnotation?.example()
        }
        if (schemaClass == String) {
            args.maxLength = schemaAnnotation?.maxLength()
            args.minLength = schemaAnnotation?.minLength()
        }
        if (schemaAnnotation?.additionalProperties() == SchemaAnnotation.AdditionalPropertiesValue.TRUE) {
            args.maxProperties = schemaAnnotation?.maxProperties()
            args.minProperties = schemaAnnotation?.minProperties()
        }
        return args
    }

    private Discriminator buildDiscriminator(String discriminatorProperty,
                                             DiscriminatorMappingAnnotation[] discriminatorMappingAnnotations) {
        if (discriminatorProperty && discriminatorMappingAnnotations) {
            Discriminator discriminator = new Discriminator()
            discriminator.setPropertyName(discriminatorProperty)
            discriminatorMappingAnnotations.each {
                if (it.schema()) {
                    Schema schema = findSchemaInOpenAPI(it.schema()) ?: buildSchema(it.schema())
                    String ref = getSchemaRef(schema)
                    discriminator.mapping(it.value(), ref)
                }
            }
            return discriminator
        }
        return null
    }

    private Map<String, Schema> buildSchemaProperties(Class clazz) {
        SortedMap<String, Schema> propMap = new TreeMap<>()
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
            Schema propSchema = findSchemaInOpenAPI(fieldType)
            if (!propSchema) {
                SchemaAnnotation schemaAnnotation = prop.field?.field?.getAnnotation(SchemaAnnotation)
                Type genericType = prop.field?.field?.genericType // Used to find out component class of Collections
                propSchema = buildSchema(schemaAnnotation, fieldType, genericType)
            }
            propMap[fieldName] = propSchema
        }
        return propMap
    }

    private OpenAPI buildOpenAPI() {
        OpenAPI openApi = new OpenAPI(
            info: getInfoFromConfig(),
            servers: swaggerConfig?.servers ?: [],
            components: getComponentsFromConfig(),
            security: swaggerConfig?.security ?: [],
            externalDocs: swaggerConfig?.externalDocs
        )
        if (swaggerConfig?.openapi) {
            // Only override default value if provided
            openApi.setOpenapi(swaggerConfig?.openapi as String)
        }
        return openApi
    }

    private Info getInfoFromConfig() {
        return new Info(
            title: swaggerConfig?.info?.title,
            summary: swaggerConfig?.info?.summary,
            description: swaggerConfig?.info?.description,
            termsOfService: swaggerConfig?.info?.termsOfService,
            contact: swaggerConfig?.info?.contact,
            license: swaggerConfig?.info?.license,
            version: swaggerConfig?.info?.version
        )
    }

    private Components getComponentsFromConfig() {
        Components components = new Components()
        Map<String, Map> securitySchemes = swaggerConfig?.components?.securitySchemes
        securitySchemes.each {String key, Map params ->
            SecurityScheme securityScheme = new SecurityScheme(
                type: SecurityScheme.Type.values().find {it.value == params.type},
                description: params.description,
                name: params.name,
                in: SecurityScheme.In.values().find {it.value == params.in},
                bearerFormat: params.bearerFormat,
                flows: params.flows ? new OAuthFlows(params.flows as Map) : null
            )
            components.addSecuritySchemes(key, securityScheme)
        }
        return components
    }

    private List<SecurityRequirement> getSecurityRequirementsFromConfig() {
        return swaggerConfig?.security?.collect { String key, List<String> val ->
            SecurityRequirement securityRequirement = new SecurityRequirement()
            securityRequirement.addList(key, val)
            return securityRequirement
        }
    }

    private NavigableMap getSwaggerConfig() {
        return Holders.config?.swagger as NavigableMap
    }

    private Schema findSchemaInOpenAPI(Class clazz) {
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
    private static boolean typeIsCommandObject(Class type) {
        return type?.name?.endsWith('Command')
    }

    @CompileStatic
    private static String getSchemaRef(Schema schema) {
        return "#/components/schemas/${schema.name}"
    }

}
