# Swagger Grails4 Plugin

## TODO

 - [x] Complete mapping of `@Tag`
 - [x] Complete mapping of `@Operation`
 - [x] Complete mapping of `@RequestBody`
 - [x] Complete mapping of `@ApiResponse`
 - [ ] Complete mapping of `@Content`
 - [ ] Support `@Callback`
 - [ ] Support `@SecurityRequirement` on class level
 - [ ] Support `@SecurityRequirement` on method level (outside of `@Operation`)
 - [ ] Support extensions
 - [ ] Response object schema

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


