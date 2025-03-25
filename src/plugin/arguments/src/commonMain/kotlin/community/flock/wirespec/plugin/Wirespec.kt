package community.flock.wirespec.plugin

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.right
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.Name
import community.flock.wirespec.plugin.files.plus
import community.flock.wirespec.plugin.files.toFilePath

fun compile(arguments: CompilerArguments) {
    val ctx = { emitter: Emitter ->
        object : CompilationContext {
            override val logger = arguments.logger
            override val emitter = emitter
        }
    }

    return arguments.input
        .flatMap { source ->
            arguments.output.plus(arguments.packageName)
                .toFilePath(source.name)
                .pairWithEmitters(arguments.emitters)
                .map { (outputFile, emitter) ->
                    ctx(emitter)
                        .compile(nonEmptyListOf(source.content))
                        .map(keepSplitOrCombine(emitter.split, outputFile))
                }
        }
        .let { either { it.bindAll() } }
        .map { it.flatten() }
        .map { it + arguments.mapShared() }
        .mapLeft { it.map(WirespecException::message) }
        .fold({ arguments.error(it.joinToString()) }) {
            it.forEach { (file, result) ->
                println("Writing ${file.name.value} to ${file.directory}")
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

    return arguments.input
        .flatMap { source ->
            (arguments.output + arguments.packageName)
                .toFilePath(source.name)
                .pairWithEmitters(arguments.emitters)
                .map { (outputFile, emitter) ->
                    ctx(emitter)
                        .emit(source.content)
                        .map(keepSplitOrCombine(emitter.split, outputFile))
                }
        }
        .let { either { it.bindAll() } }
        .map { it.flatten() }
        .map { it + arguments.mapShared() }
        .fold({ it }) {
            it.forEach { (file, result) -> arguments.writer(file, result) }
        }
}

private fun FilePath.pairWithEmitters(emitters: NonEmptySet<Emitter>) = emitters.map {
    copy(extension = it.extension) to it
}

private fun keepSplitOrCombine(split: Boolean, outputFile: FilePath) = { emitted: NonEmptyList<Emitted> ->
    when (split) {
        true -> outputFile to emitted
        false -> outputFile to nonEmptyListOf(
            Emitted(
                outputFile.name.value.firstToUpper(),
                emitted.first().result,
            ),
        )
    }
}

private fun NonEmptyList<Pair<FilePath, NonEmptyList<Emitted>>>.flatten() = flatMap { (file, results) ->
    results.map { (name, result) -> file.copy(name = Name(name)) to result }
}
