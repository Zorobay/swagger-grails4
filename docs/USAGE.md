# Usage
## Swagger configuration
Information about the swagger setup itself ([OpenAPI object](https://swagger.io/specification/?sbsearch=external-docs#openapi-object) and [Info](https://swagger.io/specification/?sbsearch=external-docs#info-object)) can be configured
through the Grails runtime configuration files (e.g `/grails-app/conf/application.yml`).

Below is an example of a configuration. Note that the entire OpenAPI spec if not yet implemented.
```yaml
swagger:
    openapi: '3.1.1' # Uses default value from OpenAPI class if not provided
    servers:
        - url: https://development.gigantic-server.com/v1
          description: Development server
        - url: https://staging.gigantic-server.com/v1
          description: Staging server
    paths: # Not implemented, as it is buildt automatically from swagger annotations
    webhooks: # Not yet implemented
    tags: # Not implemented, also built from swagger annotations
    components:
        securitySchemes: # This is the only component that is currently configureable directly from config files (the rest are built from annotations)
            BearerToken:
              type: "apiKey"
    security:
        - "BearerToken"
    externalDocs:
        description: Find more info here
        url: https://example.com
    info:
        title: Sample Pet Store App
        summary: A pet store manager.
        description: This is a sample server for a pet store.
        termsOfService: https://example.com/terms/
        contact:
            name: API Support
            url: https://www.example.com/support
            email: support@example.com
        license:
            name: Apache 2.0
            url: https://www.apache.org/licenses/LICENSE-2.0.html
        version: 1.0.1
```
### Security configuration
The following guide does a great job of explaining how to configure security: https://swagger.io/docs/specification/authentication/

This enables you to add authentication functionality to the Swagger UI, so that users can make REST requests to secure endpoints directly from Swagger.
For example, the above YAML configuration renders an _Authorize_ button which opens a modal where users can paste their token, which is automatically added to any REST request headers.

![img.png](docs/images/img.png)

## Class and method annotation
```groovy
@Tag(name = 'MyController', description = 'An example controller')
class MyController {

    @Operation(
        summary = 'An example controller',
        description = 'This controller is used to illustrate the plugin capabilities and how to use annotations.',
        parameters = [
            @Parameter(name = 'cmd', in = ParameterIn.PATH)
        ],
        responses = [
            @ApiResponse(responseCode = '200', description = 'All went OK', content = [
                @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MyResponseClass))
            ]),
            @ApiResponse(responseCode = '400', description = 'Some validation error occured. Returns a list of said validation errors.', content = [
                @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(items = @Schema(implementation = MyValidationError)))
            ])
        ]
    )
    def test(MyCommand cmd) {
        render(text: "All OK", status: HttpStatus.OK)
    }
}
```

## Command object
Input parameters of a class, which name ends in 'Command' will automatically be parsed as command objects as long as there is a matching `@Parameter` annotation
on the method, where the `name` property matches with the command parameter, see example below:

```groovy
@Operation(parameters = [
    @Parameter(name = 'cmd', in = ParameterIn.PATH)
])
def methodWithCommandObject(TestCommand cmd) {
    render(text: cmd.validate())
}
```

The parameters of the command object will be handled as separate input parameters in the Swagger documentation, of the type that is specified with the `in` property.
