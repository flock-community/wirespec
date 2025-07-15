package community.flock.wirespec.compiler.core

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.Stage.EMITTED
import community.flock.wirespec.compiler.core.Stage.PARSED
import community.flock.wirespec.compiler.core.Stage.TOKENIZED
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.HasEmitters
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Parser.parse
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.HasLogger

interface TokenizeContext :
    HasLanguageSpec,
    HasLogger

interface ParseContext :
    TokenizeContext,
    HasLogger

interface EmitContext :
    ParseContext,
    HasEmitters,
    HasLogger

interface CompilationContext :
    TokenizeContext,
    ParseContext,
    EmitContext

data class ModuleContent(val src: String, val content: String)

data class TokenizedModule(val src: String, val tokens: NonEmptyList<Token>)

fun TokenizeContext.tokenize(source: String): NonEmptyList<Token> = spec
    .tokenize(source)
    .also(TOKENIZED::log)

fun ParseContext.parse(source: NonEmptyList<ModuleContent>): EitherNel<WirespecException, AST> = parse(source.map { TokenizedModule(it.src, tokenize(it.content)) }).also(PARSED::log)

fun EmitContext.emit(ast: EitherNel<WirespecException, AST>): EitherNel<WirespecException, NonEmptyList<Emitted>> = ast
    .map { emitters.flatMap { emitter -> emitter.emit(it, logger) } }
    .also(EMITTED::log)

fun CompilationContext.compile(source: NonEmptyList<ModuleContent>): EitherNel<WirespecException, NonEmptyList<Emitted>> = emit(parse(source))

private enum class Stage {
    TOKENIZED,
    PARSED,
    EMITTED,
    ;

    fun log(it: Any): HasLogger.() -> Unit = {
        logger.debug("********** $name **********\n$it\n########## $name ##########")
    }
}
