package swagger.grails4.model

import swagger.grails4.SchemaType

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * From spec: https://swagger.io/docs/specification/data-models/data-types/
 */
class TypeAndFormat {

    SchemaType type
    String format

    TypeAndFormat(SchemaType type, String format = null) {
        this.type = type
        this.format = format
    }

    static TypeAndFormat fromClass(Class clazz) {
        switch (clazz) {
            case CharSequence:
                return new TypeAndFormat(SchemaType.STRING)
            case [short, Short, int, Integer]:
                return new TypeAndFormat(SchemaType.INTEGER, 'int32') // short (int16) is not supported by OpenAPI
            case [long, Long]:
                return new TypeAndFormat(SchemaType.INTEGER, 'int64')
            case [float, Float]:
                return new TypeAndFormat(SchemaType.NUMBER, 'float')
            case [double, Double]:
                return new TypeAndFormat(SchemaType.NUMBER, 'double')
            case Number:
                return new TypeAndFormat(SchemaType.NUMBER)
            case [boolean, Boolean]:
                return new TypeAndFormat(SchemaType.BOOLEAN)
            case { Collection.isAssignableFrom(clazz) }:
            case { clazz.isArray() }:
                return new TypeAndFormat(SchemaType.ARRAY)
            case Enum:
                return new TypeAndFormat(SchemaType.STRING)
            case [LocalDate]:
                return new TypeAndFormat(SchemaType.STRING, 'date')
            case [Date, LocalDateTime]:
                return new TypeAndFormat(SchemaType.STRING, 'date-time')
            default:
                return new TypeAndFormat(SchemaType.OBJECT)
        }
    }
}
