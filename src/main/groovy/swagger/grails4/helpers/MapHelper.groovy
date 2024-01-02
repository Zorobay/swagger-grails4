package swagger.grails4.helpers

class MapHelper {

    static Map merge(Map lowerPrecedence, Map higherPrecedence) {
        Map newMap = new HashMap()
        Set keySet = lowerPrecedence.keySet() + higherPrecedence.keySet()
        keySet.each { key ->
            def lowerVal = lowerPrecedence.get(key)
            def higherVal = higherPrecedence.get(key)
            newMap.put(key, higherVal ?: lowerVal)
        }
        return newMap
    }
}
