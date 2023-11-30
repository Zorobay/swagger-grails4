package swagger.grails4.model

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import swagger.grails4.SchemaType

import java.time.LocalDate
import java.time.LocalDateTime

class TypeAndFormatSpec extends Specification {

    @Shared
    int gStringVal = 123

    @Unroll
    void 'fromClass: finds correct type and format from: #val'() {
        when:
            TypeAndFormat res = TypeAndFormat.fromClass(val.class)
        then:
            res.type == expectedType
            res.format == expectedFormat
        where:
            val | expectedType | expectedFormat
            "${gStringVal}"                            | SchemaType.STRING  | null
            'hi!'                               | SchemaType.STRING  | null
            (1 as Short)                        | SchemaType.INTEGER | 'int32'
            (1 as short)                        | SchemaType.INTEGER | 'int32'
            (1 as Integer)                      | SchemaType.INTEGER | 'int32'
            (1 as int)                          | SchemaType.INTEGER | 'int32'
            (123 as long)                       | SchemaType.INTEGER | 'int64'
            (123 as Long)                       | SchemaType.INTEGER | 'int64'
            (123.33 as float)                   | SchemaType.NUMBER  | 'float'
            (123.33 as Float)                   | SchemaType.NUMBER  | 'float'
            (123.33 as double)                  | SchemaType.NUMBER  | 'double'
            (123.33 as Double)                  | SchemaType.NUMBER  | 'double'
            (123.33 as Number)                  | SchemaType.NUMBER  | null
            (true as boolean)                   | SchemaType.BOOLEAN | null
            (true as Boolean)                   | SchemaType.BOOLEAN | null
            [1, 2, 3]                           | SchemaType.ARRAY   | null
            ([1, 2, 3] as Integer[])            | SchemaType.ARRAY   | null
            SchemaType.STRING                   | SchemaType.STRING  | null
            LocalDate.now()                     | SchemaType.STRING  | 'date'
            new Date()                          | SchemaType.STRING  | 'date-time'
            LocalDateTime.now()                 | SchemaType.STRING  | 'date-time'
            new TypeAndFormat(SchemaType.ARRAY) | SchemaType.OBJECT  | null
    }
}
