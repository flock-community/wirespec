package community.flock.wirespec.integration.spring.kotlin.configuration

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(WirespecSerializationConfiguration::class)
annotation class EnableWirespecController
