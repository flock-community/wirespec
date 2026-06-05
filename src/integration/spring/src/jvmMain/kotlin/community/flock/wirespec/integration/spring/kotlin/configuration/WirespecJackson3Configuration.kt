package community.flock.wirespec.integration.spring.kotlin.configuration

import community.flock.wirespec.integration.jackson.v3.kotlin.WirespecSerialization
import community.flock.wirespec.integration.spring.shared.Jackson3JsonMapper
import community.flock.wirespec.integration.spring.shared.WirespecJsonMapper
import community.flock.wirespec.kotlin.Wirespec.Serialization
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule

/**
 * Active when Jackson 3 is on the classpath (Spring Boot 4). Takes precedence over
 * [WirespecJackson2Configuration], which backs off whenever Jackson 3 is present.
 */
@Configuration
// Referenced by name (not JsonMapper::class): the selection tests register this config by
// class on a Jackson-3-free classpath, where resolving a Class-valued condition would fail.
@ConditionalOnClass(name = ["tools.jackson.databind.json.JsonMapper"])
open class WirespecJackson3Configuration {

    private val jsonMapper = jsonMapper { addModule(kotlinModule()) }

    @Bean
    open fun wirespecSerialization(): Serialization = WirespecSerialization(jsonMapper)

    @Bean
    open fun wirespecJsonMapper(): WirespecJsonMapper = Jackson3JsonMapper(jsonMapper)
}
