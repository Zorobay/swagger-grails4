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

    def swagger() {
        render view: '/swagger/index'
    }

    @Operation(
            summary = 'Get the OpenAPI v3 JSON definition',
            description = 'Get the OpenAPI v3 JSON definition. Caches the generated document.',
            parameters = [
              @Parameter(name = 'param1', description = 'a cute parameter', in = ParameterIn.QUERY, required = true, allowEmptyValue = true, schema = @Schema(type = 'string') )
            ],
            responses = [
                @ApiResponse(description = 'An OpenAPI v3 JSON definition', responseCode = '200')
            ]
    )
    def test(Long param1) {
        render(text: "${param1}")
    }

    def openApiDocument() {
        OpenAPI openAPI = openApiService.generateOpenApi()
        String json = Json.pretty().writeValueAsString(openAPI)
        render(text: json, contentType: MediaType.APPLICATION_JSON, encoding: 'UTF-8')
    }
}
