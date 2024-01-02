package swagger.grails4.helpers

import spock.lang.Specification

class MapHelperSpec extends Specification {

    void 'merge'() {
        given:
            Map map1 = [
                    a: 'a',
                    c: 'c',
                    d: 123
            ]
            Map map2 = [
                    b: 'b',
                    c: null,
                    d: 321
            ]
            Map expected = [
                    a: 'a',
                    b: 'b',
                    c: 'c',
                    d: 321
            ]
        when:
            Map res = MapHelper.merge(map1, map2)
        then:
            res == expected
    }
}
