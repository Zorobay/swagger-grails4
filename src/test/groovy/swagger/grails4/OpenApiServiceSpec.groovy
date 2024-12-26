package swagger.grails4

import grails.testing.services.ServiceUnitTest
import grails.util.Holders
import io.swagger.v3.oas.integration.GenericOpenApiContext
import io.swagger.v3.oas.models.OpenAPI
import specutils.GrailsApplicationAwareSpec
import spock.lang.Specification

class OpenApiServiceSpec extends Specification implements ServiceUnitTest<OpenApiService>, GrailsApplicationAwareSpec{

    def setup() {
        Holders.grailsApplication = getGrailsApplication()
    }

    void 'generateOpenApi'() {
        when:
            OpenAPI openAPI = service.generateOpenApi()
        then:
            // OpenAPI is returned
            openAPI
            // has caching enabled
            (service.openApiContext as GenericOpenApiContext).getCacheTTL() > 0
            // no exceptions
            noExceptionThrown()
    }
}
