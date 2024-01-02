package swagger.grails4

import grails.core.DefaultGrailsApplication
import grails.util.Holders
import io.swagger.v3.oas.integration.GenericOpenApiContext
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration
import io.swagger.v3.oas.integration.api.OpenApiContext
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import swagger.grails4.openapi.GrailsReader
import swagger.grails4.openapi.GrailsScanner

class OpenApiService {

    private final OpenApiContext openApiContext = initOpenApiContext()

    OpenAPI generateOpenApi() {
        return openApiContext.read()
    }

    private static OpenApiContext initOpenApiContext() {
        // TODO don't set cache ttl to 0 (disables cache)
        OpenAPIConfiguration config = new SwaggerConfiguration().cacheTTL(0)
        OpenApiContext ctx = new GenericOpenApiContext().openApiConfiguration(config)
        DefaultGrailsApplication grailsApplication = Holders.grailsApplication as DefaultGrailsApplication
        ctx.setOpenApiScanner(new GrailsScanner(grailsApplication))
        ctx.setOpenApiReader(new GrailsReader(grailsApplication))
        ctx.init()
        return ctx
    }


}
