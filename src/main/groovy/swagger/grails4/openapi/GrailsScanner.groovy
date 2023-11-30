package swagger.grails4.openapi

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.core.GrailsClass
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration
import io.swagger.v3.oas.integration.api.OpenApiScanner

class GrailsScanner implements OpenApiScanner {

    private OpenAPIConfiguration openAPIConfiguration
    private DefaultGrailsApplication grailsApplication

    GrailsScanner(DefaultGrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    @Override
    void setConfiguration(OpenAPIConfiguration openApiConfiguration) {
        this.openAPIConfiguration = openAPIConfiguration
    }

    @Override
    Set<Class<?>> classes() {
        return grailsApplication.controllerClasses
                .findAll { GrailsClass grailsClass ->
                    // Find only controllers with Tag annotation
                    grailsClass.clazz.getAnnotation(Tag)
                }
                .collect {it.clazz} as Set<Class<?>>
    }

    @Override
    Map<String, Object> resources() {
        return null
    }
}
