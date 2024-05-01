# Swagger Grails4 Plugin

## Usage

### Configuration
### Swagger configuration

Information about the swagger setup itself ([OpenAPI object](https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#oasObject) and [Info object](https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#infoObject)) can be configured
through the Grails runtime configuration files (e.g `/grails-app/conf/application.yml)

### Class and method annotation
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

### Command object
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

## DEV notes
Personal notes helping with development: [DEV NOTES](DEV_NOTES.md)

## Contributing

### Local development

Testing the plugin locally should be easy. First, run the command:

```shell
.\gradlew.bat publishToMavenLocal
```

This will publish the plugin to your localMaven, and should be found at: `C:\Users\<user>\.m2\repository\swagger\grails4`.

To use the local plugin in another grails application, add the following to `build.gradle`:

```groovy
implementation "swagger.grails4:swagger-grails4:<version>"
```

Make sure that `mavenLocal()` is a part of the repository declarations, like so:

```groovy
repositories {
    mavenLocal()
}
```


