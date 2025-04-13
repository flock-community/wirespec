package community.flock.wirespec.compiler.core

fun String.removeBackticks() = if (hasBackticks()) removeFirstAndLastChar() else this

fun String.hasBackticks() = length > 1 && startsWith('`') && endsWith('`')

fun String.removeFirstAndLastChar() = substring(1 until length - 1)

fun String.removeQuestionMark() = if (hasQuestionMark()) removeLastChar() else this

fun String.hasQuestionMark() = length > 1 && endsWith('?')

fun String.removeLastChar() = substring(0 until length - 1)

fun String.addBackticks() = "`$this`"

fun String.orNull() = ifBlank { null }

fun String.concatGenerics() = removeJavaPrefix().removeAngularBrackets().removeCommasAndSpaces()

fun String.removeJavaPrefix() = removePrefix("java.util.")

fun String.removeAngularBrackets() = filterNot { it == '<' || it == '>' }

fun String.removeCommasAndSpaces() = filterNot { it == ',' || it == ' ' }

fun String.removeCommentMarkers(): String = removePrefix("/*").removeSuffix("*/").trim()
