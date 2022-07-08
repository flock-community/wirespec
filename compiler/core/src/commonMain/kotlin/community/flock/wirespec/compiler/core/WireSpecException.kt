package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.tokenize.Token

sealed class WireSpecException(message: String) : RuntimeException(message) {

    sealed class IOException(message: String) : WireSpecException(message) {
        class FileReadException(message: String) : IOException(message)
        class FileWriteException(message: String) : IOException(message)
    }

    sealed class CompilerException(message: String) : RuntimeException(message) {

        sealed class ParserException(string: String) : CompilerException(string) {
            class WrongTokenException(expected: Token.Type, actual: Token) :
                ParserException("$expected expected, not: ${actual.type} at line ${actual.index.line} and position ${actual.index.position - actual.value.length}")

            sealed class NullTokenException(message: String) : ParserException("$message cannot be null") {
                class StartingException : NullTokenException("Starting Token")
                class NextException : NullTokenException("Next Token")
            }
        }

        class TokenizerException(message: String) : CompilerException(message)

    }

}
