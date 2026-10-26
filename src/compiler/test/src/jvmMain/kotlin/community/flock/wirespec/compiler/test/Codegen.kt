package community.flock.wirespec.compiler.test

import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import java.io.File

public fun codegen(args: Array<String>, block: Codegen.() -> Unit) {
    require(args.size == 3) { "Usage: codegen <inputDir> <outputDir> <basePackage>" }
    Codegen(File(args[0]), File(args[1]), PackageName(args[2])).block()
}

public class Codegen internal constructor(
    public val inputDir: File,
    private val outputDir: File,
    private val basePackage: PackageName,
) {

    public fun moduleContent(file: File): ModuleContent = ModuleContent(FileUri(file.absolutePath), file.readText())

    public fun wirespec(vararg fileNames: String): AST = fileNames
        .map { moduleContent(inputDir.resolve(it)) }
        .ifEmpty {
            inputDir.walkTopDown()
                .filter { it.isFile && it.extension == "ws" }
                .map(::moduleContent)
                .toList()
        }
        .toNonEmptyListOrNull()
        .let { requireNotNull(it) { "No .ws files found under $inputDir" } }
        .let { parseContext.parse(it) }
        .fold({ error("Cannot parse wirespec: ${it.first().message}") }, { it })

    public fun emit(language: String, vararg asts: AST, emitter: (PackageName) -> Emitter): Unit = emitter(basePackage + "$language.generated")
        .let { e -> asts.flatMap { e.emit(it, noLogger) } }
        .forEach { outputDir.resolve(language).resolve(it.file).apply { parentFile.mkdirs() }.writeText(it.result) }
}

private val parseContext = object : ParseContext, NoLogger {
    override val spec = WirespecSpec
}
