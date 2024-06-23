package community.flock.wirespec.integration.spring.emit

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.transformer.EndpointClass
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class SpringKotlinEmitter(logger: Logger = noLogger,): KotlinEmitter(logger = logger) {

    override fun callAnnotation(endpointClass: EndpointClass): String {
        return when(endpointClass.method.uppercase()){
            "GET" -> "@org.springframework.web.bind.annotation.GetMapping(\"${endpointClass.path}\")\n"
            "POST" -> "@org.springframework.web.bind.annotation.PostMapping(\"${endpointClass.path}\")\n"
            "PUT" -> "@org.springframework.web.bind.annotation.PutMapping(\"${endpointClass.path}\")\n"
            "DELETE" -> "@org.springframework.web.bind.annotation.DeleteMapping(\"${endpointClass.path}\")\n"
            else -> error("Method not implemented: ${endpointClass.method}")
        }
    }
}