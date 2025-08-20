package community.flock.wirespec.integration.spring.kotlin.emit

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.emit
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.emitters.kotlin.KotlinEmitter

class SpringKotlinEmitter(packageName: PackageName) : KotlinEmitter(packageName, EmitShared(false)) {
    override fun emitHandleFunction(endpoint: Endpoint): String {
        val path = "/${endpoint.path.joinToString("/") { it.emit() }}"
        val annotation = when (endpoint.method) {
            Endpoint.Method.GET -> "@org.springframework.web.bind.annotation.GetMapping(\"${path}\")"
            Endpoint.Method.POST -> "@org.springframework.web.bind.annotation.PostMapping(\"${path}\")"
            Endpoint.Method.PUT -> "@org.springframework.web.bind.annotation.PutMapping(\"${path}\")"
            Endpoint.Method.DELETE -> "@org.springframework.web.bind.annotation.DeleteMapping(\"${path}\")"
            Endpoint.Method.PATCH -> "@org.springframework.web.bind.annotation.PatchMapping(\"${path}\")"
            Endpoint.Method.OPTIONS -> "@org.springframework.web.bind.annotation.RequestMapping(value=[\"${path}\"], method = [org.springframework.web.bind.annotation.RequestMethod.OPTIONS])"
            Endpoint.Method.HEAD -> "@org.springframework.web.bind.annotation.RequestMapping(value=[\"${path}\"], method = [org.springframework.web.bind.annotation.RequestMethod.HEAD])"
            Endpoint.Method.TRACE -> "@org.springframework.web.bind.annotation.RequestMapping(value=[\"${path}\"], method = [org.springframework.web.bind.annotation.RequestMethod.TRACE])"
        }
        return """
            |$annotation
            |${Spacer(2)}${super.emitHandleFunction(endpoint)}
            |
        """.trimMargin()
    }
}
