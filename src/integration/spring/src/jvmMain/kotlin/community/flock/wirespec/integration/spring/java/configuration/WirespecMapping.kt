package community.flock.wirespec.integration.spring.java.configuration

@Target(AnnotationTarget.FUNCTION)
annotation class WirespecMapping(val consumes: String, val produces: String)
