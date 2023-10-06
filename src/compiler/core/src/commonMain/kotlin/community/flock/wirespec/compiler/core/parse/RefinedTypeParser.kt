package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.tokenize.types.CustomRegex
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.utils.Logger

class RefinedTypeParser(logger: Logger) : AbstractParser(logger) {

    fun TokenProvider.parseRefinedType(): Either<WirespecException, Refined> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseCustomRegex(token.value).bind()
            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseCustomRegex(typeName: String) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomRegex -> Refined(typeName, Refined.Validator(token.value))
            else -> raise(WrongTokenException<CustomRegex>(token).also { eatToken().bind() })
        }.also { eatToken().bind() }
    }
}
