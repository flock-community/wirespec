package community.flock.wirespec.integration.spring.application

import community.flock.wirespec.integration.spring.configuration.WirespecConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(WirespecConfiguration::class)
open class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}