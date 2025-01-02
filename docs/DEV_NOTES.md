# DEV Notes
## Links

### OpenAPI v3 spec
https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md

### Swagger annotation docs

https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations
### Swagger UI JS library

https://github.com/swagger-api/swagger-ui/releases/tag/v5.10.3

## Publishing to Github Packages
https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry

## Publishing to Maven Central
https://central.sonatype.org/register/central-portal/#managing-your-credentials

1. Register an account at https://central.sonatype.com/
2. Register a namespace: https://central.sonatype.org/register/namespace/
3.

## Local test of plugin

Add the following to `settings.gradle`:

```gradle
include ':foo-bar'
// Relative path to foo-bar plugin
project(':foo-bar').projectDir = new File('../foo-bar')
```

Now in `build.gradle` add:

```gradle
grails {
    plugins {
        compile project (':foo-bar')
    }
}
// Another way
dependencies {
    // ... other dependencies
    compile project (':foo-bar')
}
```


Source: https://medium.com/wizpanda/another-way-of-adding-local-grails-plugin-to-a-grails-app-using-the-gradle-build-tool-d60ddaf326cb

## Development Progress

 - [x] Support `@Tag`
 - [x] Support `@Operation`
 - [x] Support `@RequestBody`
 - [x] Support `@ApiResponse`
 - [x] Support `@Content`
 - [x] Support `@MediaType`
 - [x] Support `@Encoding`
 - [x] Support `@Header`
 - [x] Complete mapping of `@Schema` (as good as completed)
   - [x] `@Schema` on model level overwrites `@Schema` on operation level
 - [x] Support `@ArraySchema`
 - [x] Support of `@Parameter`
 - [x] Support of `@Link`
 - [x] Support of `Info` from config
 - [ ] Support `@Schema` on class level
 - [ ] Support `@Schema` on class property level
 - [ ] Support `@Callback`
 - [ ] Support `@SecurityRequirement` on class level
 - [ ] Support `@SecurityRequirement` on method level (outside of `@Operation`)
 - [ ] Support extensions
 - [ ] Response object schema
 - [ ] Automatic parsing of Command object
   - [ ] Utilize existing constraints. Can be overwritten by `@Schema` on property level
