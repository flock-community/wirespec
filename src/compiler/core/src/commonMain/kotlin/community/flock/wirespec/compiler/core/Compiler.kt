package community.flock.wirespec.compiler.core

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.Stage.EMITTED
import community.flock.wirespec.compiler.core.Stage.PARSED
import community.flock.wirespec.compiler.core.Stage.TOKENIZED
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.HasEmitters
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.Parser.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.HasLogger

public interface TokenizeContext :
    HasLanguageSpec,
    HasLogger

public interface ParseContext :
    TokenizeContext,
    HasLogger

public interface EmitContext :
    ParseContext,
    HasEmitters,
    HasLogger

public interface CompilationContext :
    TokenizeContext,
    ParseContext,
    EmitContext

public data class FileUri(override val value: String) : Value<String>

public data class ModuleContent(val fileUri: FileUri, val content: String)

public data class TokenizedModule(val fileUri: FileUri, val tokens: NonEmptyList<Token>)

public fun TokenizeContext.tokenize(source: String): NonEmptyList<Token> = spec
    .tokenize(source)
    .also(TOKENIZED::log)

public fun ParseContext.parse(source: NonEmptyList<ModuleContent>): EitherNel<WirespecException, AST> = parse(source.map { TokenizedModule(it.fileUri, tokenize(it.content)) }).also(PARSED::log)

public fun EmitContext.emit(ast: EitherNel<WirespecException, AST>): EitherNel<WirespecException, NonEmptyList<Emitted>> = ast
    .map { emitters.flatMap { emitter -> emitter.emit(it, logger) } }
    .also(EMITTED::log)

public fun CompilationContext.compile(source: NonEmptyList<ModuleContent>): EitherNel<WirespecException, NonEmptyList<Emitted>> = emit(parse(source))

private enum class Stage {
    TOKENIZED,
    PARSED,
    EMITTED,
    ;

    fun log(it: Any): HasLogger.() -> Unit = {
        logger.debug("********** $name **********\n$it\n########## $name ##########")
    }
}
