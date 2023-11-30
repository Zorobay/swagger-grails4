package swagger.grails4

import grails.core.DefaultGrailsApplication
import grails.util.Holders
import io.swagger.v3.oas.integration.GenericOpenApiContext
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration
import io.swagger.v3.oas.integration.api.OpenApiContext
import io.swagger.v3.oas.models.OpenAPI
import swagger.grails4.openapi.GrailsReader
import swagger.grails4.openapi.GrailsScanner

class OpenApiService {

    OpenAPI openAPI =  new OpenAPI()
    private final OpenApiContext openApiContext = initOpenApiContext()

    OpenAPI generateOpenApi() {
        return openApiContext.read()
    }

    private OpenApiContext initOpenApiContext() {
        // TODO don't set cache ttl to 0 (disables cache)
        OpenAPIConfiguration config = new SwaggerConfiguration().openAPI(openAPI).cacheTTL(0)
        OpenApiContext ctx = new GenericOpenApiContext().openApiConfiguration(config)
        DefaultGrailsApplication grailsApplication = Holders.grailsApplication as DefaultGrailsApplication
        ctx.setOpenApiScanner(new GrailsScanner(grailsApplication))
        ctx.setOpenApiReader(new GrailsReader(grailsApplication))
        ctx.init()
        return ctx
    }
}
