package swagger.grails4.openapi

import io.swagger.v3.oas.annotations.Parameter as ParameterAnnotation
import io.swagger.v3.oas.annotations.headers.Header as HeaderAnnotation
import io.swagger.v3.oas.annotations.responses.ApiResponse as ResponseAnnotation
import io.swagger.v3.oas.annotations.links.Link as LinkAnnotation
import io.swagger.v3.oas.annotations.media.Schema as SchemaAnnotation
import io.swagger.v3.oas.models.headers.Header as HeaderModel
import io.swagger.v3.oas.models.links.Link as LinkModel
import io.swagger.v3.oas.models.media.Schema as SchemaModel
import io.swagger.v3.oas.models.parameters.Parameter as ParameterModel
import io.swagger.v3.oas.models.responses.ApiResponse as ResponseModel

class SwaggerAnnotationMapper {

    static ResponseModel mapResponseAnnotation(ResponseAnnotation responseAnnotation) {
        ResponseModel responseModel = new ResponseModel()
        // TODO map $ref, content and extensions
        responseModel.setDescription(responseAnnotation.description())
        responseModel.setHeaders(mapHeadersAnnotation(responseAnnotation.headers()))
        responseModel.setLinks(mapLinksAnnotation(responseAnnotation.links()))
//        responseModel.setContent(responseAnnotation.content())
//        responseModel.setExtensions(responseAnnotation.extensions())
        return responseModel
    }

    static ParameterModel mapParameterAnnotation(ParameterAnnotation parameterAnnotation) {
        ParameterModel parameterModel = new ParameterModel()
        // TODO $ref
        parameterModel.setName(parameterAnnotation.name())
        parameterModel.setDescription(parameterAnnotation.description())
        parameterModel.setIn(parameterAnnotation.in()?.toString())
        parameterModel.setRequired(parameterAnnotation.required())
        parameterModel.setDeprecated(parameterAnnotation.deprecated())
        parameterModel.setAllowEmptyValue(parameterAnnotation.allowEmptyValue())
        return parameterModel
    }

    static SchemaModel mapSchemaAnnotation(SchemaAnnotation schemaAnnotation) {
        SchemaModel schemaModel = new SchemaModel()
        schemaModel.type(schemaAnnotation.type())
        return schemaModel
    }

    static Map<String, HeaderModel> mapHeadersAnnotation(HeaderAnnotation[] headerAnnotations) {
        return headerAnnotations.collectEntries { HeaderAnnotation headerAnnotation ->
            HeaderModel headerModel = new HeaderModel()
            // TODO $ref
            headerModel.setDescription(headerAnnotation.description())
            headerModel.setRequired(headerAnnotation.required())
            headerModel.setDeprecated(headerAnnotation.deprecated())
            return [headerAnnotation.name(), headerModel]
        }
    }

    static Map<String, LinkModel> mapLinksAnnotation(LinkAnnotation[] linkAnnotations) {
        return linkAnnotations.collectEntries { LinkAnnotation linkAnnotation ->
            // TODO map properties
            LinkModel linkModel = new LinkModel()
            return [linkAnnotation.name(), linkModel]
        }
    }
}
