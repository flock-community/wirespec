package community.flock.wirespec.integration.spring.kotlin.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.File as LanguageFile

class SpringKotlinIrEmitter(packageName: PackageName) : KotlinIrEmitter(packageName, EmitShared(false)) {

    override fun emit(endpoint: Endpoint): LanguageFile {
        val annotation = endpoint.springAnnotation()
        fun LanguageFile.injectSpringAnnotationBeforeHandler(): LanguageFile = transform {
            injectBefore { iface: Interface ->
                if (iface.name == Name.of("Handler")) listOf(RawElement(annotation)) else emptyList()
            }
        }
        return super.emit(endpoint).injectSpringAnnotationBeforeHandler()
    }

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> {
        val results = super.emit(ast, logger)

        val modelNames = mutableListOf<String>()
        val endpointNames = mutableListOf<String>()

        ast.modules.forEach { module ->
            module.statements.forEach { definition ->
                val name = definition.identifier.value
                when (definition) {
                    is Model -> modelNames.add(name)
                    is Endpoint, is Channel -> endpointNames.add(name)
                }
            }
        }

        if (modelNames.isEmpty() && endpointNames.isEmpty()) {
            return results
        }

        val source = emitNativeHints(modelNames, endpointNames)
        val file = packageName.toDir() + "WirespecNativeHints." + extension.value
        return results + Emitted(file, source)
    }

    private fun Endpoint.springAnnotation(): String {
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

    private fun emitNativeHints(modelNames: List<String>, endpointNames: List<String>): String {
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

        val hintsFile = file("WirespecNativeHints") {
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

        return generator.generate(hintsFile)
    }
}
