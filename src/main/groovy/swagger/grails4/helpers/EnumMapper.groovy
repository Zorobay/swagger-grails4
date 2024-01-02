package swagger.grails4.helpers

import groovy.util.logging.Slf4j
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.AccessMode
import io.swagger.v3.oas.models.parameters.Parameter

@Slf4j
class EnumMapper {

    static Boolean accessModeToReadOnly(AccessMode accessMode) {
        return accessMode == AccessMode.READ_ONLY ? true : null
    }

    static Boolean accessModeToWriteOnly(AccessMode accessMode) {
        return accessMode == AccessMode.WRITE_ONLY ? true : null
    }

    static Boolean requiredModeToBoolean(Schema.RequiredMode requiredMode) {
        switch (requiredMode) {
            case Schema.RequiredMode.REQUIRED: return true
            case Schema.RequiredMode.NOT_REQUIRED: return true
            default: return false
        }
    }

    static boolean explodeToBoolean(Explode explode) {
        switch (explode) {
            case Explode.TRUE: return true
            default: return false
        }
    }

    static Parameter.StyleEnum styleEnumFromParameterStyle(ParameterStyle parameterStyle) {
        if (!parameterStyle || parameterStyle == ParameterStyle.DEFAULT) {
            return null
        }
        try {
            return Parameter.StyleEnum.valueOf(parameterStyle.name())
        } catch (IllegalArgumentException e) {
            log.error("Could not map ParameterStyle: ${parameterStyle} to StyleEnum", e)
            return null
        }
    }
}
