package community.flock.wirespec.compiler.core

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import community.flock.wirespec.compiler.core.Stage.EMITTED
import community.flock.wirespec.compiler.core.Stage.PARSED
import community.flock.wirespec.compiler.core.Stage.TOKENIZED
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.HasEmitter
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Parser.parse
import community.flock.wirespec.compiler.core.tokenize.Tokens
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
    HasEmitter,
    HasLogger

interface CompilationContext :
    TokenizeContext,
    ParseContext,
    EmitContext

fun TokenizeContext.tokenize(source: String): Tokens = spec
    .tokenize(source)
    .also(TOKENIZED::log)

fun ParseContext.parse(source: String): EitherNel<WirespecException, AST> = tokenize(source)
    .run { parse(this) }
    .also(PARSED::log)

fun EmitContext.emit(source: String): EitherNel<WirespecException, List<Emitted>> = parse(source)
    .map { emitter.emit(it, logger) }
    .also(EMITTED::log)

fun CompilationContext.compile(source: String): EitherNel<WirespecException, List<Emitted>> = emit(source)

fun CompilationContext.compile(reader: () -> String, writer: (Emitted) -> Unit, error: (String) -> Unit) {
    when (val either = compile(reader())) {
        is Left -> either.value.joinToString { it.message }.let { error(it) }
        is Right -> either.value.forEach { writer(it) }
    }
}

private enum class Stage {
    TOKENIZED,
    PARSED,
    EMITTED,
    ;

    fun log(it: Any): HasLogger.() -> Unit = {
        logger.debug("********** $name **********\n$it\n########## $name ##########")
    }
}
