package community.flock.wirespec.examples.open_api_app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OpenApiApplication

fun main(args: Array<String>) {
    runApplication<OpenApiApplication>(*args)
}
