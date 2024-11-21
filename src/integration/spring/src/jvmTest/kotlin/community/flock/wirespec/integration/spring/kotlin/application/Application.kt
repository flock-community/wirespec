package community.flock.wirespec.integration.spring.kotlin

import community.flock.wirespec.integration.spring.kotlin.configuration.EnableWirespecController
import community.flock.wirespec.integration.spring.kotlin.configuration.WirespecSerializationConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@EnableWirespecController
open class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}