package community.flock.wirespec.compiler.core

internal fun String.removeBackticks() = if (hasBackticks()) removeFirstAndLastChar() else this

internal fun String.hasBackticks() = length > 1 && startsWith('`') && endsWith('`')

private fun String.removeFirstAndLastChar() = substring(1 until length - 1)

private fun String.removeQuestionMark() = if (hasQuestionMark()) removeLastChar() else this

private fun String.hasQuestionMark() = length > 1 && endsWith('?')

private fun String.removeLastChar() = substring(0 until length - 1)

public fun String.addBackticks(): String = "`$this`"

private fun String.orNull() = ifBlank { null }

internal fun String.concatGenerics() = removeJavaPrefix().removeAngularBrackets().removeCommasAndSpaces()

private fun String.removeJavaPrefix() = removePrefix("java.util.")

private fun String.removeAngularBrackets() = filterNot { it == '<' || it == '>' }

private fun String.removeCommasAndSpaces() = filterNot { it == ',' || it == ' ' }

internal fun String.removeCommentMarkers(): String = if (startsWith("//")) {
    removePrefix("//")
} else {
    removePrefix("/*").removeSuffix("*/")
}.trim()
