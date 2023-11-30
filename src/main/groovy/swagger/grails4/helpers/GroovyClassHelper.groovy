package swagger.grails4.helpers

import grails.artefact.DomainClass

class GroovyClassHelper {

    static boolean isGroovyProperty(Class clazz, MetaProperty property) {
        switch (property.name) {
            case ~/.*(grails_|\$).*/:
            case "metaClass":
            case "properties":
            case "class":
            case "clazz":
            case "constraints":
            case "constraintsMap":
            case "mapping":
            case "log":
            case "logger":
            case "instanceControllersDomainBindingApi":
            case "instanceConvertersApi":
            case { DomainClass.isAssignableFrom(clazz) && property.name == "version" }:
            case { DomainClass.isAssignableFrom(clazz) && property.name == "transients" }:
            case { DomainClass.isAssignableFrom(clazz) && property.name == "all" }:
            case { DomainClass.isAssignableFrom(clazz) && property.name == "attached" }:
            case { DomainClass.isAssignableFrom(clazz) && property.name == "belongsTo" }:
            case { DomainClass.isAssignableFrom(clazz) && property.name == "constrainedProperties" }:
            case { DomainClass.isAssignableFrom(clazz) && property.name == "dirty" }:
            case { DomainClass.isAssignableFrom(clazz) && property.name == "dirtyPropertyNames" }:
            case { DomainClass.isAssignableFrom(clazz) && property.name == "gormDynamicFinders" }:
            case { DomainClass.isAssignableFrom(clazz) && property.name == "gormPersistentEntity" }:
                return true
            default:
                return false
        }
    }
}
