package community.flock.wirespec.lsp

import community.flock.wirespec.compiler.core.tokenize.DromedaryCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.KebabCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.Keyword
import community.flock.wirespec.compiler.core.tokenize.Method
import community.flock.wirespec.compiler.core.tokenize.PascalCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.ScreamingKebabCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.ScreamingSnakeCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.SnakeCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.SpecificType
import community.flock.wirespec.compiler.core.tokenize.TokenType
import community.flock.wirespec.compiler.core.tokenize.TypeIdentifier

object SemanticTokenLegend {
    const val TYPE_KEYWORD = 0
    const val TYPE_TYPE = 1
    const val TYPE_VARIABLE = 2
    const val TYPE_METHOD = 3

    val tokenTypes = listOf("keyword", "type", "variable", "method")
    val tokenModifiers = listOf<String>()
}

enum class TokenKind { KEYWORD, BUILT_IN_TYPE, USER_TYPE, FIELD, METHOD }

fun TokenType.toTokenKind(): TokenKind? = when (this) {
    is Keyword -> TokenKind.KEYWORD
    is SpecificType -> TokenKind.BUILT_IN_TYPE
    is PascalCaseIdentifier -> TokenKind.USER_TYPE
    // TypeIdentifier is the unrefined identifier that hasn't been narrowed to a specific built-in;
    // for our purposes treat it as a user-defined type (renameable).
    is TypeIdentifier -> TokenKind.USER_TYPE
    is Method -> TokenKind.METHOD
    is DromedaryCaseIdentifier,
    is KebabCaseIdentifier,
    is ScreamingKebabCaseIdentifier,
    is SnakeCaseIdentifier,
    is ScreamingSnakeCaseIdentifier,
        -> TokenKind.FIELD

    else -> null
}

fun TokenKind.toSemanticType(): Int = when (this) {
    TokenKind.KEYWORD -> SemanticTokenLegend.TYPE_KEYWORD
    TokenKind.BUILT_IN_TYPE -> SemanticTokenLegend.TYPE_TYPE
    TokenKind.USER_TYPE -> SemanticTokenLegend.TYPE_TYPE
    TokenKind.FIELD -> SemanticTokenLegend.TYPE_VARIABLE
    TokenKind.METHOD -> SemanticTokenLegend.TYPE_METHOD
}
