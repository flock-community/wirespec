package community.flock.wirespec.integration.spring.java.emit

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.utils.Logger

class SpringJavaEmitter(packageName: String) : JavaEmitter(PackageName(packageName)) {
    override fun emitHandleFunction(endpoint: Endpoint): String {
        val path = "/${endpoint.path.joinToString("/") { it.emit() }}"
        val annotation = when (endpoint.method) {
            Endpoint.Method.GET -> "@org.springframework.web.bind.annotation.GetMapping(\"${path}\")"
            Endpoint.Method.POST -> "@org.springframework.web.bind.annotation.PostMapping(\"${path}\")"
            Endpoint.Method.PUT -> "@org.springframework.web.bind.annotation.PutMapping(\"${path}\")"
            Endpoint.Method.DELETE -> "@org.springframework.web.bind.annotation.DeleteMapping(\"${path}\")"
            Endpoint.Method.OPTIONS -> "@org.springframework.web.bind.annotation.RequestMapping(value=\"${path}\", method = RequestMethod.OPTIONS)"
            Endpoint.Method.HEAD -> "@org.springframework.web.bind.annotation.RequestMapping(value=\"${path}\", method = RequestMethod.HEAD)"
            Endpoint.Method.PATCH -> "@org.springframework.web.bind.annotation.RequestMapping(value=\"${path}\", method = RequestMethod.PATCH)"
            Endpoint.Method.TRACE -> "@org.springframework.web.bind.annotation.RequestMapping(value=\"${path}\", method = RequestMethod.TRACE)"
        }
        return """
            |${annotation}
            |${Spacer(2)}${super.emitHandleFunction(endpoint)}
            |
        """.trimMargin()
    }
}
