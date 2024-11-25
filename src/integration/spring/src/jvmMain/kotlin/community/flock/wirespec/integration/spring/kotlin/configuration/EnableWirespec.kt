package community.flock.wirespec.integration.spring.kotlin.configuration

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@EnableWirespecController
@EnableWirespecWebClient
annotation class EnableWirespec