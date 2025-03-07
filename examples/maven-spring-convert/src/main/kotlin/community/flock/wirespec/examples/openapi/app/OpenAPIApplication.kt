package community.flock.wirespec.examples.openapi.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OpenAPIApplication

fun main(args: Array<String>) {
    runApplication<OpenAPIApplication>(*args)
}
