package swagger.grails4

class UrlMappings {

    static mappings = {

        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        get "/something/else"(controller: 'myDope', action: 'test')
        get "/swagger/test"(controller: 'swagger', action: 'test')
        get "/swagger/ui"(controller: 'swagger', action: 'ui')
        get "/swagger/openApiDocument"(controller: 'swagger', action: 'openApiDocument')
    }
}
