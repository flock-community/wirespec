package community.flock.wirespec.integration.wiremock.codegen

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import java.io.File

/**
 * Emits Java and Kotlin sources from every `.ws` file under [inputDir], into
 * `[outputDir]/{java,kotlin}/...`. Each language gets its own package — `<basePackage>.{java,kotlin}.generated`
 * — so the two source trees don't collide on type names.
 *
 * Args: <inputDir> <outputDir> <basePackage>
 */
fun main(args: Array<String>) {
    require(args.size == 3) { "Usage: codegen <inputDir> <outputDir> <basePackage>" }
    val inputDir = File(args[0])
    val outputDir = File(args[1])
    val basePackage = args[2]

    val wsFiles = inputDir.walkTopDown().filter { it.isFile && it.extension == "ws" }.toList()
    require(wsFiles.isNotEmpty()) { "No .ws files found under $inputDir" }

    val modules = wsFiles.map { ModuleContent(FileUri(it.absolutePath), it.readText()) }
    val ast = object : ParseContext, NoLogger { override val spec = WirespecSpec }
        .parse(nonEmptyListOf(modules.first(), *modules.drop(1).toTypedArray()))
        .fold({ error("Cannot parse wirespec: ${it.first().message}") }, { it })

    val javaPackage = "$basePackage.java.generated"
    val kotlinPackage = "$basePackage.kotlin.generated"
    val javaRoot = outputDir.resolve("java")
    val kotlinRoot = outputDir.resolve("kotlin")

    JavaEmitter(PackageName(javaPackage)).emit(ast, noLogger).forEach {
        javaRoot.resolve(it.file).apply { parentFile.mkdirs() }.writeText(it.result)
    }
    KotlinEmitter(PackageName(kotlinPackage)).emit(ast, noLogger).forEach {
        kotlinRoot.resolve(it.file).apply { parentFile.mkdirs() }.writeText(it.result)
    }
}
