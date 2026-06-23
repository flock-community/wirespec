package community.flock.wirespec.integration.spring.kotlin.configuration

import org.springframework.context.annotation.Import

/**
 * Enables a Spring-backed [community.flock.wirespec.kotlin.Wirespec.Transportation] (configured through
 * Spring's `HttpServiceProxyFactory`) plus the Wirespec serialization beans, so generated
 * `<Endpoint>.Call` clients can be wired up.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(WirespecSerializationConfiguration::class, WirespecTransportationConfiguration::class)
annotation class EnableWirespecTransportation
