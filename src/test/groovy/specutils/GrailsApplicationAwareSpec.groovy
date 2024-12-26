package specutils

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.gorm.validation.ConstrainedProperty
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.web.mapping.DefaultUrlMappingData
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.grails.web.mapping.RegexUrlMapping
import org.springframework.context.support.StaticApplicationContext

trait GrailsApplicationAwareSpec {

    DefaultGrailsApplication getGrailsApplication(List<Class> controllers = [], List<MockUrlMapping> urlMappings = []) {
        StaticApplicationContext applicationContext = new StaticApplicationContext()
        applicationContext.beanFactory.registerSingleton('grailsUrlMappingsHolder', new DefaultUrlMappingsHolder(urlMappings))
        DefaultGrailsApplication grailsApplication = new DefaultGrailsApplication()
        grailsApplication.registerArtefactHandler(new ControllerArtefactHandler())
        controllers.each {
            grailsApplication.addArtefact(it)
        }
        grailsApplication.setMainContext(applicationContext)
        applicationContext.refresh()
        return grailsApplication
    }

    DefaultUrlMappingsHolder getUrlMappingsHolder(GrailsApplication grailsApplication) {
        return grailsApplication.mainContext.getBean('grailsUrlMappingsHolder') as DefaultUrlMappingsHolder
    }

    static class MockUrlMapping extends RegexUrlMapping {

        MockUrlMapping(String controller, String action, String httpMethod = null) {
            super(new DefaultUrlMappingData("/${controller}/${action}"), controller, action, null, null, null, httpMethod, null, [] as ConstrainedProperty[], null)
        }

    }
}

