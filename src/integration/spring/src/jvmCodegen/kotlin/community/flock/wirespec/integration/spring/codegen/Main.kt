package community.flock.wirespec.integration.spring.codegen

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.integration.spring.extension.SpringMappingAnnotationsExtension
import community.flock.wirespec.integration.spring.extension.SpringNativeHintsExtension
import community.flock.wirespec.ir.extension.applyExtensions
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import java.io.File

/**
 * Petstore (OpenAPI) + todo.ws → Spring-annotated Java + Kotlin test sources.
 * Args: <inputDir> <outputDir> <basePackage>
 */
fun main(args: Array<String>) {
    require(args.size == 3) { "Usage: codegen <inputDir> <outputDir> <basePackage>" }
    val inputDir = File(args[0])
    val outputDir = File(args[1])
    val basePackage = PackageName(args[2])

    val petstore = inputDir.resolve("petstore.json")
        .let { OpenAPIV3Parser.parse(ModuleContent(FileUri(it.absolutePath), it.readText()), false) }
    val todo = inputDir.resolve("todo.ws")
        .let { ModuleContent(FileUri(it.absolutePath), it.readText()) }
        .let { parseContext.parse(nonEmptyListOf(it)) }
        .fold({ error("Cannot parse wirespec: ${it.first().message}") }, { it })

    val kotlinPkg = basePackage + "kotlin.generated"
    val javaPkg = basePackage + "java.generated"

    KotlinIrEmitter(kotlinPkg, EmitShared(false))
        .applyExtensions(
            listOf(
                SpringMappingAnnotationsExtension(FileExtension.Kotlin),
                SpringNativeHintsExtension(kotlinPkg, FileExtension.Kotlin),
            ),
        )
        .emitAll(outputDir.resolve("kotlin"), petstore, todo)

    JavaIrEmitter(javaPkg, EmitShared(false))
        .applyExtensions(
            listOf(
                SpringMappingAnnotationsExtension(FileExtension.Java),
                SpringNativeHintsExtension(javaPkg, FileExtension.Java),
            ),
        )
        .emitAll(outputDir.resolve("java"), petstore, todo)
}

private val parseContext = object : ParseContext, NoLogger {
    override val spec = WirespecSpec
}

private fun Emitter.emitAll(root: File, vararg asts: AST) = asts
    .flatMap { emit(it, noLogger) }
    .forEach { root.resolve(it.file).apply { parentFile.mkdirs() }.writeText(it.result) }
