package swagger.grails4

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import swagger.grails4.deleteme.TestErrorResponse

@Tag(name = 'MyDope', description = 'A test controller')
class MyDopeController {

    @Operation(
            summary = 'Get the OpenAPI v3 JSON definition',
            description = 'Get the OpenAPI v3 JSON definition. Caches the generated document.',
            parameters = [
                    @Parameter(name = 'len', description = 'length of random string to generate', in = ParameterIn.QUERY, required = true, allowEmptyValue = true, schema = @Schema(type = 'integer')),
                    @Parameter(name = 'characters', description = 'characters to generate string from', in = ParameterIn.QUERY, style = ParameterStyle.SPACEDELIMITED, required = true, allowEmptyValue = true)
            ],
            responses = [
                    @ApiResponse(description = 'A random string', responseCode = '200'),
                    @ApiResponse(description = 'error response', responseCode = '400', content = [
                            @Content(mediaType = "application/json", schema = @Schema(implementation = TestErrorResponse))
                    ]
                    )
            ]
    )
    def randStr(int len, ArrayList<String> characters) {
        Random random = new Random()
        String randStr = ''
        (1..len).each {
            int i = random.nextInt(characters.size())
            randStr += characters[i]
        }
        render(text: randStr)
    }

    @Operation(
            parameters = [
                    @Parameter(name = 'password', description = 'Should have password format, string type',
                            schema = @Schema(type = 'string', format = 'password'))
            ]
    )
    def method(String password) {
        println(password)
    }

    @Operation()
    def test() {

    }
}
