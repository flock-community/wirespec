package community.flock.wirespec.plugin

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.right
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import community.flock.wirespec.plugin.Language.Java
import community.flock.wirespec.plugin.Language.Kotlin
import community.flock.wirespec.plugin.Language.OpenAPIV2
import community.flock.wirespec.plugin.Language.OpenAPIV3
import community.flock.wirespec.plugin.Language.Scala
import community.flock.wirespec.plugin.Language.TypeScript
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.files.File
import community.flock.wirespec.plugin.files.FileName
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.inferOutputFile
import community.flock.wirespec.plugin.files.plus
import community.flock.wirespec.plugin.files.toJSONFile
import community.flock.wirespec.plugin.files.toJavaFile
import community.flock.wirespec.plugin.files.toKotlinFile
import community.flock.wirespec.plugin.files.toScalaFile
import community.flock.wirespec.plugin.files.toTypeScriptFile
import community.flock.wirespec.plugin.files.toWirespecFile

fun compile(arguments: CompilerArguments) {
    val ctx = { emitter: Emitter ->
        object : CompilationContext {
            override val logger = arguments.logger
            override val emitter = emitter
        }
    }

    return arguments.inputFiles
        .flatMap { inputFile ->
            arguments.outputDirectory.plus(arguments.packageName)
                .inferOutputFile(inputFile)
                .pairWithEmitters(arguments.languages, arguments.packageName)
                .map { (outputFile, emitter) ->
                    ctx(emitter)
                        .compile(arguments.reader(inputFile))
                        .map(keepSplitOrCombine(emitter.split, outputFile))
                }
        }
        .let { either { it.bindAll() } }
        .map { it.flatten() }
        .map { it + arguments.mapShared() }
        .mapLeft { it.map(WirespecException::message) }
        .fold({ arguments.error(it.joinToString()) }) {
            it.forEach { (file, result) ->
                println("Writing ${file.path.fileName.value} to ${file.path.directory}")
                arguments.writer(file, result)
            }
        }
}

fun convert(arguments: ConverterArguments) {
    val ctx = { emitter: Emitter ->
        object {
            fun emit(source: String) = emitter.emit(
                parser(source, arguments.strict),
                arguments.logger,
            ).right()

            private val parser = when (arguments.format) {
                Format.OpenAPIV2 -> OpenAPIV2Parser::parse
                Format.OpenAPIV3 -> OpenAPIV3Parser::parse
                Format.Avro -> AvroParser::parse
            }
        }
    }

    return arguments.inputFiles
        .flatMap { inputFile ->
            arguments.outputDirectory.plus(arguments.packageName)
                .inferOutputFile(inputFile)
                .pairWithEmitters(arguments.languages, arguments.packageName)
                .map { (outputFile, emitter) ->
                    ctx(emitter)
                        .emit(arguments.reader(inputFile))
                        .map(keepSplitOrCombine(emitter.split, outputFile))
                }
        }
        .let { either { it.bindAll() } }
        .map { it.flatten() }
        .fold({ it }) {
            it.forEach { (file, result) -> arguments.writer(file, result) }
        }
}

private fun FilePath.pairWithEmitters(languages: NonEmptySet<Language>, packageName: PackageName) = languages.map {
    when (it) {
        Java -> toJavaFile() to JavaEmitter(packageName)
        Kotlin -> toKotlinFile() to KotlinEmitter(packageName)
        Scala -> toScalaFile() to ScalaEmitter(packageName)
        TypeScript -> toTypeScriptFile() to TypeScriptEmitter()
        Wirespec -> toWirespecFile() to WirespecEmitter()
        OpenAPIV2 -> toJSONFile() to OpenAPIV2Emitter
        OpenAPIV3 -> toJSONFile() to OpenAPIV3Emitter
    }
}

private fun keepSplitOrCombine(split: Boolean, outputFile: File) = { emitted: NonEmptyList<Emitted> ->
    when (split) {
        true -> outputFile to emitted
        false -> outputFile to nonEmptyListOf(
            Emitted(
                outputFile.path.fileName.value.firstToUpper(),
                emitted.first().result,
            ),
        )
    }
}

private fun NonEmptyList<Pair<File, NonEmptyList<Emitted>>>.flatten() = flatMap { (file, results) ->
    results.map { (name, result) -> file.change(FileName(name)) to result }
}
