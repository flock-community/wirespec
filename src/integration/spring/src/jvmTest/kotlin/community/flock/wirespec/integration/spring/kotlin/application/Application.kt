package community.flock.wirespec.integration.spring.kotlin.application

import community.flock.wirespec.integration.spring.kotlin.configuration.EnableWirespec
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableWirespec
open class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
