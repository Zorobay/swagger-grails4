# Swagger Grails4 Plugin

## Installation
...

## Configuration

The plugin does not define any URL-mappings for the [`SwaggerController`](grails-app/controllers/swagger/grails4/SwaggerController.groovy).
It is therefore up to the user to define it in their `UrlMapping.groovy`, for example:

```groovy
"/swagger/$action"(controller: 'swagger')
"/swagger"(controller: 'swagger', action: 'ui') // Access 'ui' action directly from /swagger
```
## Usage

YAML configuration and annotation usage: [USAGE](docs/USAGE.md)

## DEV notes
Personal notes helping with development: [DEV NOTES](docs/DEV_NOTES.md)

## Contributing

### Local development

**This method is better: https://medium.com/wizpanda/another-way-of-adding-local-grails-plugin-to-a-grails-app-using-the-gradle-build-tool-d60ddaf326cb**

Testing the plugin locally should be easy. First, run the command:

```shell
.\gradlew.bat publishToMavenLocal
```

This will publish the plugin to your localMaven, and should be found at: `C:\Users\<user>\.m2\repository\swagger\grails4`.

To use the local plugin in another grails application, add the following to `build.gradle`:

```groovy
implementation "io.github.zorobay:swagger-grails4:<version>"
```

Make sure that `mavenLocal()` is a part of the repository declarations, like so:

```groovy
repositories {
    mavenLocal()
}
```


