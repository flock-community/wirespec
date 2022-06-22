package community.flock.wirespec

sealed class WireSpecException(message: String) : RuntimeException(message) {

    sealed class IOException(message: String) : WireSpecException(message) {
        class FileReadException(message: String) : IOException(message)
        class FileWriteException(message: String) : IOException(message)
    }

    sealed class CompilerException(message: String) : RuntimeException(message) {

        class EmitterException(message: String) : CompilerException(message)

        class ParserException(message: String) : CompilerException(message)

        class TokenizerException(message: String) : CompilerException(message)

    }

}
