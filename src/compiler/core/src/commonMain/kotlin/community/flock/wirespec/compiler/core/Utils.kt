package community.flock.wirespec.compiler.core

public fun String.addBackticks(): String = "`$this`"

internal fun String.removeBackticks() = if (hasBackticks()) removeFirstAndLastChar() else this

internal fun String.hasBackticks() = length > 1 && startsWith('`') && endsWith('`')

private fun String.removeFirstAndLastChar() = substring(1 until length - 1)

internal fun String.concatGenerics() = removeJavaPrefix().removeAngularBrackets().removeCommasAndSpaces()

private fun String.removeJavaPrefix() = removePrefix("java.util.")

private fun String.removeAngularBrackets() = filterNot { it == '<' || it == '>' }

private fun String.removeCommasAndSpaces() = filterNot { it == ',' || it == ' ' }

internal fun String.removeCommentMarkers() = (
    takeIf { it.startsWith("//") }
        ?.removePrefix("//")
        ?: removePrefix("/*").removeSuffix("*/")
    ).trim()
