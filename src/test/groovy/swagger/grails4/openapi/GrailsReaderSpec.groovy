package swagger.grails4.openapi

import grails.core.DefaultGrailsApplication
import grails.validation.Validateable
import io.swagger.v3.oas.models.media.Schema
import junit.framework.TestCase
import org.grails.testing.GrailsApplicationBuilder
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.StaticApplicationContext
import spock.lang.Specification

class GrailsReaderSpec extends Specification {

    void 'buildSchemaProperties: '() {
        given:
            StaticApplicationContext applicationContext = new StaticApplicationContext()
            applicationContext.beanFactory.registerSingleton('grailsUrlMappingsHolder', new DefaultUrlMappingsHolder([]))
            DefaultGrailsApplication grailsApplication = new DefaultGrailsApplication()
            grailsApplication.setMainContext(applicationContext)
            applicationContext.refresh()
            GrailsReader grailsReader = new GrailsReader(grailsApplication)
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
