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

fun TokenType.toSemanticType(): Int? = when (this) {
    is Keyword -> SemanticTokenLegend.TYPE_KEYWORD
    is SpecificType -> SemanticTokenLegend.TYPE_TYPE
    is TypeIdentifier, is PascalCaseIdentifier -> SemanticTokenLegend.TYPE_TYPE
    is Method -> SemanticTokenLegend.TYPE_METHOD
    is DromedaryCaseIdentifier,
    is KebabCaseIdentifier,
    is ScreamingKebabCaseIdentifier,
    is SnakeCaseIdentifier,
    is ScreamingSnakeCaseIdentifier,
        -> SemanticTokenLegend.TYPE_VARIABLE

    else -> null
}
