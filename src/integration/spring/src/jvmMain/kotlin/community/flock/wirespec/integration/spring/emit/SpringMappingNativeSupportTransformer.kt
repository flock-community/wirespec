package community.flock.wirespec.integration.spring.emit

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.transformer.IrTransformer
import community.flock.wirespec.ir.core.File as LanguageFile

/**
 * Emits a WirespecNativeHints file registering all models and endpoints for
 * GraalVM native images.
 *
 * The hints class body is hand-written source and so differs per [language].
 * Register alongside [SpringMappingAnnotationsSupportTransformer] on a Kotlin
 * or Java [community.flock.wirespec.ir.emit.IrEmitter].
 */
open class SpringMappingNativeSupportTransformer(
    private val packageName: PackageName,
    private val language: FileExtension,
) : IrTransformer {

    override fun transform(ir: IR, ast: AST): IR = ir + listOfNotNull(nativeHintsFile(ast))

    private fun nativeHintsFile(ast: AST): LanguageFile? {
        val definitions = ast.modules.toList().flatMap { it.statements }
        val modelNames = definitions.filterIsInstance<Model>().map { Name.of(it.identifier.value).pascalCase() }
        val endpointNames = definitions
            .filter { it is Endpoint || it is Channel }
            .map { Name.of(it.identifier.value).pascalCase() }

        if (modelNames.isEmpty() && endpointNames.isEmpty()) return null

        val pkg = packageName.value
        return file(Name.of(packageName.toDir() + "WirespecNativeHints")) {
            `package`(pkg)
            modelNames.forEach { import("$pkg.model", it) }
            endpointNames.forEach { import("$pkg.endpoint", it) }
            import("org.springframework.aot.hint", "MemberCategory")
            import("org.springframework.aot.hint", "RuntimeHints")
            import("org.springframework.aot.hint", "RuntimeHintsRegistrar")
            import("org.springframework.context.annotation", "Configuration")
            import("org.springframework.context.annotation", "ImportRuntimeHints")
            raw(nativeHintsClassBody(modelNames + endpointNames))
        }
    }

    private fun nativeHintsClassBody(names: List<String>): String = when (language) {
        FileExtension.Kotlin -> kotlinNativeHintsClassBody(names)
        FileExtension.Java -> javaNativeHintsClassBody(names)
        else -> error("SpringMappingNativeSupportTransformer supports Kotlin and Java targets only, got $language")
    }

    private fun kotlinNativeHintsClassBody(names: List<String>): String {
        val registrations = names.joinToString("\n") { "      registerWithInnerClasses(hints, $it::class.java, allMembers)" }
        return """
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
    }

    private fun javaNativeHintsClassBody(names: List<String>): String {
        val registrations = names.joinToString("\n") { "      registerWithInnerClasses(hints, $it.class, allMembers);" }
        return """
            |@Configuration
            |@ImportRuntimeHints(WirespecNativeHints.GeneratedHints.class)
            |public class WirespecNativeHints {
            |
            |  static class GeneratedHints implements RuntimeHintsRegistrar {
            |    @Override
            |    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            |      var allMembers = MemberCategory.values();
            |$registrations
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
        """.trimMargin()
    }
}
