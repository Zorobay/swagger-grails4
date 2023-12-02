package swagger.grails4

class UrlMappings {

    static mappings = {

        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }


        get "/something/else"(controller: 'myDope', action: 'test')

        get "/swagger/"(controller: 'swagger', action: 'index')
        get "/swagger/ui"(controller: 'swagger', action: 'ui')
        get "/swagger/openApiDocument"(controller: 'swagger', action: 'openApiDocument')
    }
}
