package community.flock.wirespec.integration.spring.kotlin.application

import community.flock.wirespec.integration.spring.kotlin.configuration.WirespecConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(WirespecConfiguration::class)
open class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}