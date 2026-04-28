package community.flock.wirespec.integration.spring.kotlin.emit

import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.File as LanguageFile

internal fun LanguageFile.injectSpringAnnotationBeforeHandler(annotation: String): LanguageFile = transform {
    injectBefore { iface: Interface ->
        if (iface.name == Name.of("Handler")) listOf(RawElement(annotation)) else emptyList()
    }
}

internal fun Endpoint.springAnnotation(): String {
    val pathString = "/" + path.joinToString("/") {
        when (it) {
            is Endpoint.Segment.Literal -> it.value
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
        }
    }
    return when (method) {
        Endpoint.Method.GET -> "@org.springframework.web.bind.annotation.GetMapping(\"$pathString\")"
        Endpoint.Method.POST -> "@org.springframework.web.bind.annotation.PostMapping(\"$pathString\")"
        Endpoint.Method.PUT -> "@org.springframework.web.bind.annotation.PutMapping(\"$pathString\")"
        Endpoint.Method.DELETE -> "@org.springframework.web.bind.annotation.DeleteMapping(\"$pathString\")"
        Endpoint.Method.PATCH -> "@org.springframework.web.bind.annotation.PatchMapping(\"$pathString\")"
        Endpoint.Method.OPTIONS -> "@org.springframework.web.bind.annotation.RequestMapping(value=[\"$pathString\"], method = [org.springframework.web.bind.annotation.RequestMethod.OPTIONS])"
        Endpoint.Method.HEAD -> "@org.springframework.web.bind.annotation.RequestMapping(value=[\"$pathString\"], method = [org.springframework.web.bind.annotation.RequestMethod.HEAD])"
        Endpoint.Method.TRACE -> "@org.springframework.web.bind.annotation.RequestMapping(value=[\"$pathString\"], method = [org.springframework.web.bind.annotation.RequestMethod.TRACE])"
    }
}
