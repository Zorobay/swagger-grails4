package swagger.grails4

import grails.testing.web.controllers.ControllerUnitTest
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.http.MediaType
import spock.lang.Specification

class SwaggerControllerSpec extends Specification implements ControllerUnitTest<SwaggerController> {

    def setup() {
        controller.openApiService = Mock(OpenApiService)
    }

    void 'index: redirects to ui'() {
        when:
            controller.index()
        then:
            response.redirectedUrl == '/swagger/ui'
    }

    void 'ui: renders correct view'() {
        when:
            controller.ui()
        then:
            view == '/swagger/swagger-ui'
    }

    void 'openApiDocument'() {
        when:
            controller.openApiDocument()
        then:
            1 * controller.openApiService.generateOpenApi() >> new OpenAPI(info: new Info(title: 'test'))
            response.json.info.title == 'test'
            response.contentType == MediaType.APPLICATION_JSON_VALUE
    }
}
