package swagger.grails4

import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import org.springframework.http.MediaType

class SwaggerController {

    OpenApiService openApiService

    def index() {
        redirect(action: 'ui')
    }

    def ui() {
        render view: '/swagger/swagger-ui'
    }

    def openApiDocument() {
        OpenAPI openAPI = openApiService.generateOpenApi()
        String json = Json.pretty().writeValueAsString(openAPI)
        render(text: json, contentType: MediaType.APPLICATION_JSON, encoding: 'UTF-8')
    }
}
