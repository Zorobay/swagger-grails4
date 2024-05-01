package swagger.grails4

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import swagger.grails4.deleteme.TestCommand
import swagger.grails4.deleteme.TestErrorResponse

@Tag(name = 'My Test Controller', description = 'A test controller')
class TestController {

    @Operation(
        responses = [
            @ApiResponse(responseCode = '200', description = 'All is well', content = [
                @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TestErrorResponse))
            ]),
            @ApiResponse(responseCode = '400', description = 'Some validation error occured', content = [
                @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(items = @Schema(implementation = TestCommand)))
            ])
        ]
    )
    def testArraySchema() {

    }
}
