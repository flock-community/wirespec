package community.flock.wirespec.integration.spring.java.emit

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.utils.Logger

class SpringJavaEmitter(packageName: String, logger: Logger) : JavaEmitter(packageName, logger) {
    override fun emitHandleFunction(endpoint: Endpoint): String {
        val path = "/${endpoint.path.joinToString("/") { it.emit() }}"
        val annotation = when (endpoint.method) {
            Endpoint.Method.GET -> "@org.springframework.web.bind.annotation.GetMapping(\"${path}\")\n"
            Endpoint.Method.POST -> "@org.springframework.web.bind.annotation.PostMapping(\"${path}\")\n"
            Endpoint.Method.PUT -> "@org.springframework.web.bind.annotation.PutMapping(\"${path}\")\n"
            Endpoint.Method.DELETE -> "@org.springframework.web.bind.annotation.DeleteMapping(\"${path}\")\n"
            else -> error("Method not implemented: ${endpoint.method}")
        }
        return """
            |${annotation}
            |${super.emitHandleFunction(endpoint)}
        """.trimMargin()
    }
}