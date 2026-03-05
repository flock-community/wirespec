package community.flock.wirespec.integration.spring.kotlin.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.emit
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.utils.Logger
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

    private fun emitNativeHints(modelNames: List<String>, endpointNames: List<String>): String {
        val pkg = packageName.value
        val modelImports = modelNames.joinToString("\n") { "import $pkg.model.$it" }
        val endpointImports = endpointNames.joinToString("\n") { "import $pkg.endpoint.$it" }
        val modelRegistrations = modelNames.joinToString("\n") { "      registerWithInnerClasses(hints, $it::class.java, allMembers)" }
        val endpointRegistrations = endpointNames.joinToString("\n") { "      registerWithInnerClasses(hints, $it::class.java, allMembers)" }

        return """
            |package $pkg
            |
            |$modelImports
            |$endpointImports
            |
            |import org.springframework.aot.hint.MemberCategory
            |import org.springframework.aot.hint.RuntimeHints
            |import org.springframework.aot.hint.RuntimeHintsRegistrar
            |import org.springframework.context.annotation.Configuration
            |import org.springframework.context.annotation.ImportRuntimeHints
            |
            |@Configuration
            |@ImportRuntimeHints(WirespecNativeHints.GeneratedHints::class)
            |open class WirespecNativeHints {
            |
            |  class GeneratedHints : RuntimeHintsRegistrar {
            |    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
            |      val allMembers = MemberCategory.entries.toTypedArray()
            |$modelRegistrations
            |$endpointRegistrations
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
            |
        """.trimMargin()
    }
}
