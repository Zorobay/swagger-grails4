package swagger.grails4

import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.models.OpenAPI
import org.springframework.http.MediaType

@Tag(name = "Swagger", description = "A fun little controller")
class SwaggerController {

    OpenApiService openApiService

    def index() {
        redirect(action: 'ui')
    }

    def ui() {
        render view: '/swagger/index'
    }

    def openApiDocument() {
        OpenAPI openAPI = openApiService.generateOpenApi()
        String json = Json.pretty().writeValueAsString(openAPI)
        render(text: json, contentType: MediaType.APPLICATION_JSON, encoding: 'UTF-8')
    }
}
