package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import community.flock.wirespec.plugin.io.FilePath
import kotlin.reflect.KFunction2

fun compile(arguments: CompilerArguments) {
    val ctx = {
        object : CompilationContext {
            override val logger = arguments.logger
            override val emitters = arguments.emitters
        }
    }

    println(arguments.emitters.map { it.extension.value })

    ctx().compile(arguments.input.map { ModuleContent(it.name.value, it.content) })
        .mapLeft { it.map(WirespecException::message) }
        .fold({ arguments.error(it.joinToString()) }) {
            it.forEach { (file, result) ->
                println(arguments.output.path.value)
                arguments.writer(FilePath(arguments.output.path.value + "/" + file), result)
            }
        }
}

fun convert(arguments: ConverterArguments) {
    val parser: KFunction2<ModuleContent, Boolean, AST> = when (arguments.format) {
        Format.OpenAPIV2 -> OpenAPIV2Parser::parse
        Format.OpenAPIV3 -> OpenAPIV3Parser::parse
        Format.Avro -> AvroParser::parse
    }

    arguments.input
        .map { ModuleContent(it.name.value, it.content) }
        .map { moduleContent -> parser.invoke(moduleContent, arguments.strict) }
        .flatMap { ast -> arguments.emitters.flatMap { it.emit(ast, arguments.logger) } }
        .forEach { (file, result) ->
            arguments.writer(FilePath(arguments.output.path.value + "/" + file), result) }
}
