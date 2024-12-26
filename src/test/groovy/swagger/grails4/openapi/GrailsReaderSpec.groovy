package swagger.grails4.openapi

import grails.artefact.Artefact
import grails.core.DefaultGrailsApplication
import grails.util.Holders
import grails.validation.Validateable
import grails.web.Action
import grails.web.Controller
import io.swagger.v3.oas.annotations.ExternalDocumentation as ExternalDocumentationAnnotation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.SecurityScheme
import org.grails.config.PropertySourcesConfig
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import specutils.GrailsApplicationAwareSpec
import spock.lang.Shared
import spock.lang.Specification

class GrailsReaderSpec extends Specification implements GrailsApplicationAwareSpec {

    @Shared
    List<MockUrlMapping> URL_MAPPINGS = [
        new MockUrlMapping(TestController.logicalPropertyName, 'putAction', HttpMethod.PUT.name()),
        new MockUrlMapping(TestController.logicalPropertyName, 'getAction', HttpMethod.GET.name())
    ]

    void setupSpec() {
        Holders.config = new PropertySourcesConfig()
    }

    void 'read: reads and applies configs correctly to create Info'() {
        given:
            GrailsReader grailsReader = createGrailsReader()
            Map swaggerConfigs = [
                info: [
                    title         : 'my title',
                    summary       : 'my summary',
                    description   : 'my description',
                    termsOfService: 'my tos',
                    contact       : [
                        name : 'my contact name',
                        url  : 'my contact url',
                        email: 'my contact email'
                    ],
                    license       : [
                        name: 'my license name',
                        url : 'my license url'
                    ],
                    version       : 'my version'
                ]
            ]
            Holders.config.swagger = swaggerConfigs
        when:
            OpenAPI res = grailsReader.read([] as Set<Class<?>>, [:])
        then:
            res.info.title == 'my title'
            res.info.summary == 'my summary'
            res.info.description == 'my description'
            res.info.termsOfService == 'my tos'
            res.info.contact.name == 'my contact name'
            res.info.contact.url == 'my contact url'
            res.info.contact.email == 'my contact email'
            res.info.license.name == 'my license name'
            res.info.license.url == 'my license url'
            res.info.version == 'my version'
    }

    void 'read: reads and applies configs correctly to create Servers'() {
        given:
            GrailsReader grailsReader = createGrailsReader()
            Map swaggerConfigs = [
                servers: [
                    [
                        url        : 'http://www.server1.com/{myvar}/{myvar2}',
                        description: 'my server description1',
                        variables  : [
                            myvar : [enum: ['a', 'b'], default: 'a', description: 'myvar description'],
                            myvar2: [enum: ['c', 'd'], default: 'd', description: 'myvar2 description'],
                        ]
                    ],
                    [
                        url        : 'http://www.server2.com/{myvar}',
                        description: 'my server description2',
                        variables  : [
                            myvar: [enum: ['a', 'b'], default: 'a', description: 'myvar description']
                        ]
                    ]
                ]
            ]
            Holders.config.swagger = swaggerConfigs
        when:
            OpenAPI res = grailsReader.read([] as Set<Class<?>>, [:])
        then:
            res.servers.size() == 2
            res.servers[0].url == 'http://www.server1.com/{myvar}/{myvar2}'
            res.servers[0].description == 'my server description1'
            res.servers[0].variables.size() == 2
            res.servers[0].variables.myvar.enum == ['a', 'b']
            res.servers[0].variables.myvar.default == 'a'
            res.servers[0].variables.myvar.description == 'myvar description'
            res.servers[0].variables.myvar2.enum == ['c', 'd']
            res.servers[0].variables.myvar2.default == 'd'
            res.servers[0].variables.myvar2.description == 'myvar2 description'
            res.servers[1].url == 'http://www.server2.com/{myvar}'
            res.servers[1].description == 'my server description2'
            res.servers[1].variables.size() == 1
            res.servers[1].variables.myvar.enum == ['a', 'b']
            res.servers[1].variables.myvar.default == 'a'
            res.servers[1].variables.myvar.description == 'myvar description'
    }

    void 'read: reads and applies configs correctly to create Components'() {
        given:
            GrailsReader grailsReader = createGrailsReader()
            Map swaggerConfigs = [
                components: [
                    securitySchemes: [
                        BearerToken: [
                            type        : "apiKey",
                            description : 'apiKey description',
                            name        : 'apiKey name',
                            in          : 'header',
                            bearerFormat: 'JWT'
                        ],
                        OAuth2     : [
                            type       : 'oauth2',
                            description: 'oauth2 description',
                            name       : 'oauth2 name',
                            in         : 'query',
                            flows      : [
                                implicit         : [
                                    authorizationUrl: 'http://www.auth.com',
                                    tokenUrl        : 'http://www.token.com',
                                    refreshUrl      : 'http://www.refresh.com',
                                    scopes          : [a: 'a']
                                ],
                                password         : [
                                    authorizationUrl: 'http://www.auth.com',
                                    tokenUrl        : 'http://www.token.com',
                                    refreshUrl      : 'http://www.refresh.com',
                                    scopes          : [a: 'a']
                                ],
                                clientCredentials: [
                                    authorizationUrl: 'http://www.auth.com',
                                    tokenUrl        : 'http://www.token.com',
                                    refreshUrl      : 'http://www.refresh.com',
                                    scopes          : [a: 'a']
                                ],
                                authorizationCode: [
                                    authorizationUrl: 'http://www.auth.com',
                                    tokenUrl        : 'http://www.token.com',
                                    refreshUrl      : 'http://www.refresh.com',
                                    scopes          : [a: 'a']
                                ],

                            ]
                        ]
                    ]
                ]
            ]
            Holders.config.swagger = swaggerConfigs
        when:
            OpenAPI res = grailsReader.read([] as Set<Class<?>>, [:])
        then:
            res.components.securitySchemes.size() == 2
            res.components.securitySchemes.BearerToken.type == SecurityScheme.Type.APIKEY
            res.components.securitySchemes.BearerToken.description == 'apiKey description'
            res.components.securitySchemes.BearerToken.name == 'apiKey name'
            res.components.securitySchemes.BearerToken.in == SecurityScheme.In.HEADER
            res.components.securitySchemes.BearerToken.bearerFormat == 'JWT'
            res.components.securitySchemes.OAuth2.type == SecurityScheme.Type.OAUTH2
            res.components.securitySchemes.OAuth2.description == 'oauth2 description'
            res.components.securitySchemes.OAuth2.name == 'oauth2 name'
            res.components.securitySchemes.OAuth2.in == SecurityScheme.In.QUERY
            with(res.components.securitySchemes.OAuth2.flows) { OAuthFlows flows ->
                ['implicit', 'password', 'clientCredentials', 'authorizationCode'].every { String key ->
                    OAuthFlow flow = flows.getAt(key)
                    return flow.authorizationUrl == 'http://www.auth.com' &&
                        flow.tokenUrl == 'http://www.token.com' &&
                        flow.refreshUrl == 'http://www.refresh.com' &&
                        flow.scopes.size() == 1 && flow.scopes.a == 'a'
                }
            }
    }

    void 'read: reads and applies configs correctly to create SecurityRequirements'() {
        given:
            GrailsReader grailsReader = createGrailsReader()
            Map swaggerConfigs = [
                security: [
                    [
                        petstore_auth: [
                            "write:pets",
                            "read:pets"
                        ]
                    ],
                    [
                        humanstore_auth: [
                            "write:humans",
                            "read:humans"
                        ]
                    ]
                ]
            ]
            Holders.config.swagger = swaggerConfigs
        when:
            OpenAPI res = grailsReader.read([] as Set<Class<?>>, [:])
        then:
            res.security.size() == 2
            res.security[0]['petstore_auth'].size() == 2
            res.security[0]['petstore_auth'][0] == 'write:pets'
            res.security[0]['petstore_auth'][1] == 'read:pets'
            res.security[1]['humanstore_auth'].size() == 2
            res.security[1]['humanstore_auth'][0] == 'write:humans'
            res.security[1]['humanstore_auth'][1] == 'read:humans'
    }

    void 'read: reads and applies configs correctly to create ExternalDocumentation'() {
        given:
            GrailsReader grailsReader = createGrailsReader()
            Map swaggerConfigs = [
                externalDocs: [
                    description: 'my description',
                    url        : 'http://www.url.com'
                ]
            ]
            Holders.config.swagger = swaggerConfigs
        when:
            OpenAPI res = grailsReader.read([] as Set<Class<?>>, [:])
        then:
            res.externalDocs.description == 'my description'
            res.externalDocs.url == 'http://www.url.com'
    }

    void 'read: overrides "openapi" if provided'() {
        given:
            GrailsReader grailsReader = createGrailsReader()
            Map swaggerConfigs = [openapi: 'override me']
            Holders.config.swagger = swaggerConfigs
        when:
            OpenAPI res = grailsReader.read([] as Set<Class<?>>, [:])
        then:
            res.openapi == 'override me'
    }

    void 'read: handles no configs'() {
        given:
            GrailsReader grailsReader = createGrailsReader()
            Map swaggerConfigs = [:]
            Holders.config.swagger = swaggerConfigs
        when:
            OpenAPI res = grailsReader.read([] as Set<Class<?>>, [:])
        then:
            res
            noExceptionThrown()
    }

    void 'read: handles controller Tag annotations'() {
        given:
            DefaultGrailsApplication grailsApplication = getGrailsApplication([TestController])
            GrailsReader grailsReader = new GrailsReader(grailsApplication)
        when:
            OpenAPI res = grailsReader.read([TestController] as Set<Class<?>>, [:])
        then:
            res.tags.size() == 1
            res.tags[0].name == 'testController'
            res.tags[0].description == 'test description'
            res.tags[0].externalDocs.description == 'ext description'
            res.tags[0].externalDocs.url == 'http://www.extdocs.com'
    }

    void 'read: handles controller operation/action annotations'() {
        given:
            DefaultGrailsApplication grailsApplication = getGrailsApplication([TestController], URL_MAPPINGS)
            GrailsReader grailsReader = new GrailsReader(grailsApplication)
            String controllerName = TestController.logicalPropertyName
            String putUrl = "/${controllerName}/putAction"
            String getUrl = "/${controllerName}/getAction"
        when:
            OpenAPI res = grailsReader.read([TestController] as Set<Class<?>>, [:])
        then:
            res.paths.size() == 2
            with(res.paths[putUrl].put) {
                it.tags == ['t1', 't2']
                it.summary == 'action summary'
                it.description == 'action description'
            }
            with(res.paths[getUrl].get) {
                it.tags == ['testController'] // Gets tag from @Tag annotation if missing from operation
            }
    }

    void 'buildSchemaProperties: '() {
        given:
            GrailsReader grailsReader = createGrailsReader()
        when:
            Map<String, Schema> res = grailsReader.buildSchemaProperties(TestClass)
        then:
            res.size() == 4
            with(res.name) {
                assert it.name == String.canonicalName
                assert it.type == 'string'
                assert !it.format
                assert !it.properties
            }
            with(res.number) {
                assert it.name == Integer.canonicalName
                assert it.type == 'integer'
                assert it.format == 'int32'
                assert it.description == 'This is always number 3'
                assert it.enum == ['3']
                assert !it.properties
            }
            with(res.testEnum) {
                assert it.name == TestEnum.canonicalName
                assert it.type == 'string'
                assert !it.format
                assert it.enum == TestEnum.values()*.name()
                assert !it.properties
            }
            with(res.aList) {
                assert it.name == List.canonicalName
                assert it.type == 'array'
                assert !it.format
                assert !it.properties
                with(it.items) {
                    assert it.name == SubClass.canonicalName
                    assert it.type == 'object'
                    assert !it.format
                    assert it.properties.keySet().size() == 2
                    with(it.properties.isWorking) {
                        assert it.name == boolean.canonicalName
                        assert it.type == 'boolean'
                        assert !it.format
                        assert !it.properties
                    }
                    with(it.properties.aMap) {
                        assert it.name == Map.canonicalName
                        assert it.type == 'object'
                        assert !it.format
                        assert !it.properties
                        assert (it.additionalProperties as Schema).type == ''
                    }
                }
            }
    }

    void 'buildExternalDocumentation'() {
        given:
            ExternalDocumentationAnnotation externalDocumentation = Mock(ExternalDocumentationAnnotation) {
                description() >> 'my description'
                url() >> 'http://www.url.com'
                extensions() >> [] // not yet supported
            }
            GrailsReader grailsReader = createGrailsReader()
        when:
            ExternalDocumentation res = grailsReader.buildExternalDocumentation(externalDocumentation)
        then:
            res.description == 'my description'
            res.url == 'http://www.url.com'
            !res.extensions
    }

    GrailsReader createGrailsReader() {
        return new GrailsReader(getGrailsApplication())
    }

    @Tag(name = 'testController', description = 'test description', externalDocs = @ExternalDocumentationAnnotation(
        description = 'ext description', url = 'http://www.extdocs.com'
    ))
    @Artefact("Controller")
    @Controller
    private class TestController {

        static String logicalPropertyName = 'grailsReaderSpec$Test'

        @Operation(method = 'PUT', tags = ['t1', 't2'], summary = 'action summary', description = 'action description')
        @Action
        void putAction() {
            render(status: HttpStatus.OK)
        }

        @Operation(method = 'GET')
        @Action
        void getAction() {

        }
    }

    private enum TestEnum {

        FIRST('first'),
        SECOND('second')

        String val

        TestEnum(String val) {
            this.val = val
        }
    }

    private class TestClass implements Validateable {

        String name
        @io.swagger.v3.oas.annotations.media.Schema(description = 'This is always number 3', allowableValues = ['3'])
        Integer number = 3
        TestEnum testEnum
        List<SubClass> aList = []
    }

    private class SubClass {

        boolean isWorking = false
        Map<String, SubSubClass> aMap
    }

    private class SubSubClass {

        Double value
    }
}
