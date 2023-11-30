package swagger.grails4

enum SchemaType {

    STRING('string'),
    INTEGER('integer'),
    BOOLEAN('boolean'),
    NUMBER('number'),
    ARRAY('array'),
    OBJECT('object')

    String name

    SchemaType(String name) {
        this.name = name
    }

    static SchemaType fromSwaggerName(String type) {
        return values().find {it.name == type}
    }
}
