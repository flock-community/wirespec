package community.flock.wirespec.compiler.core.parse

import arrow.core.nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.tokenize.types.CustomRegex
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.utils.Logger

class RefinedTypeParser(logger: Logger):AbstractParser(logger) {

     fun TokenProvider.parseRefinedType() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseCustomRegex(token.value).bind()
            else -> raise(
                WirespecException.CompilerException.ParserException.WrongTokenException(CustomType::class, token).also { eatToken().bind() }.nel())
        }
    }

    private fun TokenProvider.parseCustomRegex(typeName: String) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomRegex -> Refined(typeName, Refined.Validator(token.value))
            else -> raise(
                WirespecException.CompilerException.ParserException.WrongTokenException(CustomRegex::class, token).also { eatToken().bind() }.nel())
        }.also { eatToken().bind() }
    }
}
