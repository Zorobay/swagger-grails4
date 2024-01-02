# Swagger Grails4 Plugin

## Usage

### Configuration

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

## Development Progress

 - [x] Complete mapping of `@Tag`
 - [x] Complete mapping of `@Operation`
 - [x] Complete mapping of `@RequestBody`
 - [x] Complete mapping of `@ApiResponse`
 - [ ] Complete mapping of `@Content`
 - [ ] Complete mapping of `@MediaType`
 - [x] Complete mapping of `@Schema` (as good as completed)
   - [x] `@Schema` on model level overwrites `@Schema` on operation level
 - [ ] Support `@ArraySchema`
 - [x] Complete mapping of `@Parameter`
 - [ ] Complete support of `Info` from config
 - [ ] Support `@Schema` on class level
 - [ ] Support `@Schema` on class property level
 - [ ] Support `@Callback`
 - [ ] Support `@SecurityRequirement` on class level
 - [ ] Support `@SecurityRequirement` on method level (outside of `@Operation`)
 - [ ] Support extensions
 - [ ] Response object schema
 - [ ] Automatic parsing of Command object
   - [ ] Utilize existing constraints. Can be overwritten by `@Schema` on property level

## Links

### OpenAPI v3 spec
https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md

### Swagger annotation docs

https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations
### Swagger UI JS library

https://github.com/swagger-api/swagger-ui/releases/tag/v5.10.3

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

Make sure that `mavenLocal()` is a part of the buildscript repositories, like so:

```groovy
buildscript {
    repositories {
        mavenLocal()
    }
}
```


