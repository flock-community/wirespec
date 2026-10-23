package community.flock.wirespec.integration.jvm.codegen

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.scala.ScalaEmitter
import community.flock.wirespec.integration.jvm.extension.BuilderExtension
import java.io.File

/**
 * Wirespec → Java + Kotlin + Scala source emitter, with builders, for the jvm module's tests.
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
    val ast = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }
        .parse(nonEmptyListOf(modules.first(), *modules.drop(1).toTypedArray()))
        .fold({ error("Cannot parse wirespec: ${it.first().message}") }, { it })

    JavaEmitter(PackageName("$basePackage.java.generated"), EmitShared(false))
        .applyExtensions(nonEmptyListOf(BuilderExtension(FileExtension.Java)))
        .emit(ast, noLogger)
        .forEach { outputDir.resolve("java").resolve(it.file).apply { parentFile.mkdirs() }.writeText(it.result) }
    KotlinEmitter(PackageName("$basePackage.kotlin.generated"), EmitShared(false))
        .applyExtensions(nonEmptyListOf(BuilderExtension(FileExtension.Kotlin)))
        .emit(ast, noLogger)
        .forEach { outputDir.resolve("kotlin").resolve(it.file).apply { parentFile.mkdirs() }.writeText(it.result) }
    ScalaEmitter(PackageName("$basePackage.scala.generated"), EmitShared(false))
        .applyExtensions(nonEmptyListOf(BuilderExtension(FileExtension.Scala)))
        .emit(ast, noLogger)
        .forEach { outputDir.resolve("scala").resolve(it.file).apply { parentFile.mkdirs() }.writeText(it.result) }
}
