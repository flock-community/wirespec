package community.flock.wirespec.integration.spring.kotlin.emit

import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.transformer.EndpointClass
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class SpringKotlinEmitter(packageName: String = DEFAULT_GENERATED_PACKAGE_STRING, logger: Logger = noLogger) : KotlinEmitter(packageName, logger) {
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