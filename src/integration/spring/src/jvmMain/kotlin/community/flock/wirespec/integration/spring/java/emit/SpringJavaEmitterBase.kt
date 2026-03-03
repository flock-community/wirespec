package community.flock.wirespec.integration.spring.java.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.emitters.java.JavaEmitter

abstract class SpringJavaEmitterBase(packageName: PackageName) : JavaEmitter(packageName, EmitShared(false)) {

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
        val file = "${packageName.toDir()}WirespecNativeHints.${extension.value}"
        return results + Emitted(file, source)
    }

    private fun emitNativeHints(modelNames: List<String>, endpointNames: List<String>): String {
        val pkg = packageName.value
        val modelImports = modelNames.joinToString("\n") { "import $pkg.model.$it;" }
        val endpointImports = endpointNames.joinToString("\n") { "import $pkg.endpoint.$it;" }
        val modelRegistrations = modelNames.joinToString("\n") { "      registerWithInnerClasses(hints, $it.class, allMembers);" }
        val endpointRegistrations = endpointNames.joinToString("\n") { "      registerWithInnerClasses(hints, $it.class, allMembers);" }

        return """
            |package $pkg;
            |
            |$modelImports
            |$endpointImports
            |
            |import org.springframework.aot.hint.MemberCategory;
            |import org.springframework.aot.hint.RuntimeHints;
            |import org.springframework.aot.hint.RuntimeHintsRegistrar;
            |import org.springframework.context.annotation.Configuration;
            |import org.springframework.context.annotation.ImportRuntimeHints;
            |
            |@Configuration
            |@ImportRuntimeHints(WirespecNativeHints.GeneratedHints.class)
            |public class WirespecNativeHints {
            |
            |  static class GeneratedHints implements RuntimeHintsRegistrar {
            |    @Override
            |    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            |      var allMembers = MemberCategory.values();
            |$modelRegistrations
            |$endpointRegistrations
            |    }
            |
            |    private static void registerWithInnerClasses(RuntimeHints hints, Class<?> clazz, MemberCategory[] categories) {
            |      hints.reflection().registerType(clazz, categories);
            |      for (Class<?> inner : clazz.getDeclaredClasses()) {
            |        registerWithInnerClasses(hints, inner, categories);
            |      }
            |    }
            |  }
            |}
            |
        """.trimMargin()
    }
}
