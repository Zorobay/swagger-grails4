package swagger.grails4.deleteme

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = 'description on class level', title='title on class level')
class TestInputBody {

    String name
    Boolean isWicked
    List<TestInputSubBody> subs = []
}

class TestInputSubBody {
    String name

    @Schema(defaultValue = '10', maximum = '100', example = "22")
    int age = 10
}
