package community.flock.wirespec.integration.spring.emit

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.findAll
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.extension.IrExtension
import community.flock.wirespec.ir.core.File as LanguageFile

/**
 * Adds Spring MVC mapping annotations (`@GetMapping`, `@PostMapping`, ...) to
 * every endpoint Handler.
 *
 * The IR is language-neutral, so the matching is shared across targets; only
 * the array-valued `@RequestMapping` syntax for OPTIONS/HEAD/TRACE differs per
 * [language]. Register alongside [SpringMappingNativeSupportExtension] on a
 * Kotlin or Java [community.flock.wirespec.ir.emit.IrEmitter].
 */
open class SpringMappingAnnotationsSupportExtension(
    private val language: FileExtension,
) : IrExtension {

    override fun transform(ir: IR, ast: AST): IR {
        val endpoints = ast.modules.toList().flatMap { it.statements }.filterIsInstance<Endpoint>()
        return ir.map { element ->
            if (element is LanguageFile) element.annotateEndpointHandler(endpoints) else element
        }
    }

    private fun LanguageFile.annotateEndpointHandler(endpoints: List<Endpoint>): LanguageFile {
        val endpoint = endpoints.find { containsEndpointNamespace(it) } ?: return this
        return injectSpringAnnotationBeforeHandler(endpoint.springAnnotation(language))
    }

    private fun LanguageFile.containsEndpointNamespace(endpoint: Endpoint): Boolean = findAll<Namespace>().any {
        it.extends?.name?.value() == "Wirespec.Endpoint" && it.name.pascalCase() == Name.of(endpoint.identifier.value).pascalCase()
    }
}

internal fun LanguageFile.injectSpringAnnotationBeforeHandler(annotation: String): LanguageFile = transform {
    injectBefore { iface: Interface ->
        if (iface.name == Name.of("Handler")) listOf(RawElement(annotation)) else emptyList()
    }
}

internal fun Endpoint.springAnnotation(language: FileExtension): String {
    val pathString = "/" + path.joinToString("/") {
        when (it) {
            is Endpoint.Segment.Literal -> it.value
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
        }
    }
    val prefix = "org.springframework.web.bind.annotation"
    fun requestMapping(method: String): String = when (language) {
        FileExtension.Kotlin -> "@$prefix.RequestMapping(value=[\"$pathString\"], method = [$prefix.RequestMethod.$method])"
        else -> "@$prefix.RequestMapping(value=\"$pathString\", method = $prefix.RequestMethod.$method)"
    }
    return when (method) {
        Endpoint.Method.GET -> "@$prefix.GetMapping(\"$pathString\")"
        Endpoint.Method.POST -> "@$prefix.PostMapping(\"$pathString\")"
        Endpoint.Method.PUT -> "@$prefix.PutMapping(\"$pathString\")"
        Endpoint.Method.DELETE -> "@$prefix.DeleteMapping(\"$pathString\")"
        Endpoint.Method.PATCH -> "@$prefix.PatchMapping(\"$pathString\")"
        Endpoint.Method.OPTIONS -> requestMapping("OPTIONS")
        Endpoint.Method.HEAD -> requestMapping("HEAD")
        Endpoint.Method.TRACE -> requestMapping("TRACE")
    }
}
