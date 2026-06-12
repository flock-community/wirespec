package community.flock.wirespec.integration.spring.kotlin.emit

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.core.findAll
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.transformer.IrTransformer
import community.flock.wirespec.ir.core.File as LanguageFile

/**
 * Adds Spring MVC mapping annotations to every endpoint Handler and emits a
 * WirespecNativeHints file registering all models and endpoints for GraalVM
 * native images. Register alongside a Kotlin [community.flock.wirespec.ir.emit.IrEmitter].
 */
class SpringKotlinIrTransformer(private val packageName: PackageName) : IrTransformer {

    override fun transform(ir: IR, ast: AST): IR {
        val endpoints = ast.modules.toList().flatMap { it.statements }.filterIsInstance<Endpoint>()
        val annotated = ir.map { element ->
            if (element is LanguageFile) element.annotateEndpointHandler(endpoints) else element
        }
        return annotated + listOfNotNull(nativeHintsFile(ast))
    }

    private fun LanguageFile.annotateEndpointHandler(endpoints: List<Endpoint>): LanguageFile {
        val endpoint = endpoints.find { containsEndpointNamespace(it) } ?: return this
        return injectSpringAnnotationBeforeHandler(endpoint.springAnnotation())
    }

    private fun LanguageFile.containsEndpointNamespace(endpoint: Endpoint): Boolean = findAll<Namespace>().any {
        it.extends?.name?.value() == "Wirespec.Endpoint" && it.name.pascalCase() == Name.of(endpoint.identifier.value).pascalCase()
    }

    private fun nativeHintsFile(ast: AST): LanguageFile? {
        val definitions = ast.modules.toList().flatMap { it.statements }
        val modelNames = definitions.filterIsInstance<Model>().map { Name.of(it.identifier.value).pascalCase() }
        val endpointNames = definitions
            .filter { it is Endpoint || it is Channel }
            .map { Name.of(it.identifier.value).pascalCase() }

        if (modelNames.isEmpty() && endpointNames.isEmpty()) return null

        val pkg = packageName.value
        val registrations = (modelNames + endpointNames)
            .joinToString("\n") { "      registerWithInnerClasses(hints, $it::class.java, allMembers)" }

        val classBody = """
            |@Configuration
            |@ImportRuntimeHints(WirespecNativeHints.GeneratedHints::class)
            |open class WirespecNativeHints {
            |
            |  class GeneratedHints : RuntimeHintsRegistrar {
            |    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
            |      val allMembers = MemberCategory.entries.toTypedArray()
            |$registrations
            |    }
            |
            |    private fun registerWithInnerClasses(hints: RuntimeHints, clazz: Class<*>, categories: Array<MemberCategory>) {
            |      hints.reflection().registerType(clazz, *categories)
            |      for (inner in clazz.declaredClasses) {
            |        registerWithInnerClasses(hints, inner, categories)
            |      }
            |    }
            |  }
            |}
        """.trimMargin()

        return file(Name.of(packageName.toDir() + "WirespecNativeHints")) {
            `package`(pkg)
            modelNames.forEach { import("$pkg.model", it) }
            endpointNames.forEach { import("$pkg.endpoint", it) }
            import("org.springframework.aot.hint", "MemberCategory")
            import("org.springframework.aot.hint", "RuntimeHints")
            import("org.springframework.aot.hint", "RuntimeHintsRegistrar")
            import("org.springframework.context.annotation", "Configuration")
            import("org.springframework.context.annotation", "ImportRuntimeHints")
            raw(classBody)
        }
    }
}

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
