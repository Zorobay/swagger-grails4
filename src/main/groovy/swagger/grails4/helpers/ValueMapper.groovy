package swagger.grails4.helpers

import grails.compiler.GrailsCompileStatic

@GrailsCompileStatic
class ValueMapper {

    static BigDecimal stringToBigDecimal(String val) {
        if (val) {
            return new BigDecimal(val)
        }
        return null
    }

    static BigDecimal intToBigDecimal(Integer val) {
        return val ? new BigDecimal(val) : null
    }
}
