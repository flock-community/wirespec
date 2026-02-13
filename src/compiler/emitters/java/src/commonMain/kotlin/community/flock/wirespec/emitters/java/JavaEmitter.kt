package community.flock.wirespec.emitters.java

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.LanguageEmitter
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.utils.Logger

interface JavaEmitters :
    JavaIdentifierEmitter,
    JavaTypeDefinitionEmitter,
    JavaEndpointDefinitionEmitter,
    JavaChannelDefinitionEmitter,
    JavaEnumDefinitionEmitter,
    JavaUnionDefinitionEmitter,
    JavaRefinedTypeDefinitionEmitter

open class JavaEmitter(
    override val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : JavaEmitters, LanguageEmitter() {

    val import =
        """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.java.Wirespec;
        |
        """.trimMargin()

    override val extension = FileExtension.Java

    override val shared = JavaShared

    override val singleLineComment = "//"

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> =
        super<LanguageEmitter>.emit(module, logger).let {
            if (emitShared.value) it + Emitted(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.java").toDir() + "Wirespec", shared.source)
            else it
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): Emitted =
        super<LanguageEmitter>.emit(definition, module, logger).let {
            val subPackageName = packageName + definition
            val (allImports, processedCode) = processJavaImports(it.result, module.needImports())
            val importStatements = buildImportStatements(allImports)
            Emitted(
                file = subPackageName.toDir() + it.file.sanitizeSymbol(),
                result =
                    """
                    |package $subPackageName;
                    |
                    |${importStatements.trim()}
                    |
                    |${processedCode.trim()}
                    |
                    """.trimMargin().trimStart()
            )
        }

    private fun processJavaImports(code: String, needsWirespecImport: Boolean): Pair<List<String>, String> {
        // Extract existing imports from the code
        val importPattern = Regex("""^\s*import\s+([^;]+);\n""", RegexOption.MULTILINE)
        val existingImports = importPattern.findAll(code)
            .map { it.groupValues[1].trim() }
            .toMutableSet()

        // Remove existing import statements from code
        var processedCode = importPattern.replace(code, "")

        // Add Wirespec import if needed
        if (needsWirespecImport) {
            existingImports.add("$DEFAULT_SHARED_PACKAGE_STRING.java.Wirespec")
        }

        // Pattern to match java.util.*, java.io.*, java.time.*, etc. including nested packages
        // Matches: java.util.List, java.util.regex.Pattern, java.util.concurrent.CompletableFuture, etc.
        // Does not match java.lang.* which is auto-imported
        val javaFqnPattern = Regex("""java\.(util|io|time|math|net|nio)(\.[a-z]+)*\.[A-Z][a-zA-Z0-9]*""")

        // Find all unique fully qualified names in the code
        val javaImports = javaFqnPattern.findAll(processedCode)
            .map { it.value }
            .toSet()

        // Add java imports to existing imports
        existingImports.addAll(javaImports)

        // Replace each FQN with its simple name
        javaImports.forEach { fqn ->
            val simpleName = fqn.substringAfterLast('.')
            processedCode = processedCode.replace(fqn, simpleName)
        }

        // Sort imports according to project conventions:
        // 1. custom/project imports (alphabetically)
        // 2. blank line
        // 3. java/javax imports (alphabetically)
        val (standardImports, customImports) = existingImports
            .sorted()
            .partition { it.startsWith("java.") || it.startsWith("javax.") }

        val sortedImports = buildList {
            addAll(customImports)
            if (standardImports.isNotEmpty() && customImports.isNotEmpty()) {
                add("") // Blank line separator
            }
            addAll(standardImports)
        }

        return sortedImports to processedCode
    }

    private fun buildImportStatements(imports: List<String>): String {
        return if (imports.isEmpty()) {
            ""
        } else {
            imports.joinToString("\n") { import ->
                if (import.isEmpty()) "" else "import $import;"
            }
        }
    }
}
