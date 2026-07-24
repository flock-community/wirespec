package community.flock.wirespec.examples.kotest

import community.flock.wirespec.integration.spring.kotlin.configuration.EnableWirespecController
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@EnableKafka
@EnableWirespecController
@SpringBootApplication
class CampaignApplication

fun main(args: Array<String>) {
    runApplication<CampaignApplication>(*args)
}
