package community.flock.wirespec.examples.spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProjectManagementApplication

fun main(args: Array<String>) {
    runApplication<ProjectManagementApplication>(*args)
}
