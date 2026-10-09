package community.flock.wirespec.compiler.core.exceptions

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.tokenize.Token

internal sealed interface Error {
    val message: String
}

public sealed class WirespecException(public val fileUri: FileUri, override val message: String, public val coordinates: Token.Coordinates) : Error

internal class DefaultsNotSupportedException(fileUri: FileUri, language: String, fields: NonEmptyList<String>) :
    WirespecException(
        fileUri,
        "$language does not support default values, but these fields have one: ${fields.joinToString()}. " +
            "Remove the defaults, or leave them out of the generated code with the ignore defaults option: " +
            "--ignore-defaults for the CLI, or ignoreDefaults in the Gradle and Maven plugins.",
        Token.Coordinates(),
    )
