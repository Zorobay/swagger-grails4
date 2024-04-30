# Swagger Grails4 Plugin

## Usage

### Configuration

### Class and method annotation
```groovy
@Tag(name = 'MyController', description = 'A test controller')
class MyController {
    @Operation(
            summary = 'A test method summary',
            description = 'A longer description of this test method',
            parameters = [
                    @Parameter(name = 'len', description = 'length of random string to generate', in = ParameterIn.QUERY, required = true, allowEmptyValue = true, schema = @Schema(type = 'integer')),
                    @Parameter(name = 'characters', description = 'characters to generate string from', in = ParameterIn.QUERY, style = ParameterStyle.SPACEDELIMITED, required = true, allowEmptyValue = true)
            ],
            responses = [
                    @ApiResponse(description = 'A random string', responseCode = '200'),
                    @ApiResponse(description = 'error response', responseCode = '400', content = [
                            @Content(mediaType = "application/json", schema = @Schema(implementation = TestErrorResponse))
                    ]
                    )
            ]
    )
    def test(int len, List<String> characters) {
        render(text: "random string")
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


