package community.flock.wirespec.compiler.core.tokenize

data class TokenizeOptions(
    val removeWhitespace: Boolean = true,
    val specifyTypes: Boolean = true,
    val specifyFieldIdentifiers: Boolean = true,
)
