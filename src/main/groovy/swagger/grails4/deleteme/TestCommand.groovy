package swagger.grails4.deleteme

import grails.validation.Validateable
import io.swagger.v3.oas.annotations.media.Schema

class TestCommand implements Validateable {

    @Schema(maxLength = 100, example = 'mamma')
    String a
    @Schema(minLength = 1, description = 'Antal ints')
    List<Integer> ints
    @Schema(allowableValues = ['a', 'b', 'c'])
    String b

    static constraints = {
        a nullable: false, maxSize: 100
        ints nullable: false, minSize: 1
    }
}
