package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.tokenize.Token

sealed class WireSpecException(message: String, val index: Token.Index) : RuntimeException(message) {

    sealed class IOException(message: String) : WireSpecException(message, Token.Index()) {
        class FileReadException(message: String) : IOException(message)
        class FileWriteException(message: String) : IOException(message)
    }

    sealed class CompilerException(message: String, index: Token.Index) : WireSpecException(message, index) {

        sealed class ParserException(index: Token.Index, message: String) : CompilerException(message, index) {
            class WrongTokenException(expected: Token.Type, actual: Token) :
                ParserException(
                    actual.index,
                    "$expected expected, not: ${actual.type} at line ${actual.index.line} and position ${actual.index.position - actual.value.length}"
                )

            sealed class NullTokenException(message: String, index: Token.Index) :
                ParserException(index, "$message cannot be null") {
                class StartingException : NullTokenException("Starting Token", Token.Index())
                class NextException(index: Token.Index) : NullTokenException("Next Token", index)
            }
        }

        class TokenizerException(index: Token.Index, message: String) : CompilerException(message, index)

    }

}
