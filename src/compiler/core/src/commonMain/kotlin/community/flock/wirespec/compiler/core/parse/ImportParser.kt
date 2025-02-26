package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.WsCustomType
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.Literal
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecType
import community.flock.wirespec.compiler.utils.Logger

class ImportParser(logger: Logger) : AbstractParser(logger) {

    fun TokenProvider.parseImport(): Either<WirespecException, Import> = either {
        eatToken().bind()
        token.log()
        val references = when (token.type) {
            is LeftCurly -> parseReferences(listOf()).bind()
            else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
        }
        val url = when (token.type) {
            is Literal -> token.value.drop(1).dropLast(1).also { eatToken().bind() }
            else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
        }
        when {
            !Regex("^[\\w,\\s-]+\\.ws\$").matches(url) -> raise(ParserException.RelativeImportException(token))
            else -> Import(
                url = Import.Url(url),
                references = references.toNonEmptyListOrNull() ?: raise(ParserException.EmptyImportReferenceException(token)),
            )
        }
    }

    private fun TokenProvider.parseReferences(list: List<Reference.Custom>): Either<WirespecException, List<Reference.Custom>> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is WsCustomType -> parseReferences(list + Reference.Custom(token.value, false)).bind()
            is Comma -> parseReferences(list).bind()
            is RightCurly -> list.also { eatToken().bind() }
            else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
        }
    }
}
