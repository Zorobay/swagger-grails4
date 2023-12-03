package swagger.grails4.enums

enum SchemaType {

    STRING('string'),
    INTEGER('integer'),
    BOOLEAN('boolean'),
    NUMBER('number'),
    ARRAY('array'),
    OBJECT('object'),
    UNKNOWN('')

    String swaggerName

    SchemaType(String swaggerName) {
        this.swaggerName = swaggerName
    }

    static SchemaType fromSwaggerName(String type) {
        return values().find {it.swaggerName == type}
    }
}
