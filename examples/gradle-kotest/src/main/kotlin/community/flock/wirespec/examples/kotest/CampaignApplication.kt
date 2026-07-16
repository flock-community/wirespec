package community.flock.wirespec.examples.kotest

import community.flock.wirespec.integration.spring.kotlin.configuration.EnableWirespecController
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * A small Spring Boot app whose HTTP surface and async messages are both defined by a
 * single Wirespec contract (`src/main/wirespec/campaign.ws`).
 *
 * `@EnableWirespecController` turns the generated `*.Handler` interfaces into Spring MVC
 * controllers and registers the Wirespec JSON (de)serialization — including a
 * `Wirespec.Serialization` bean the Kafka layer reuses so wire formats stay identical
 * across REST and messaging.
 */
@SpringBootApplication
@EnableWirespecController
open class CampaignApplication

fun main(args: Array<String>) {
    runApplication<CampaignApplication>(*args)
}
