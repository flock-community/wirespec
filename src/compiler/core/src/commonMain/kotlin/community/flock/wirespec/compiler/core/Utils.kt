package community.flock.wirespec.compiler.core

fun String.removeBackticks() = if (hasBackticks()) removeFirstAndLastChar() else this

fun String.hasBackticks() = length > 1 && startsWith('`') && endsWith('`')

fun String.removeFirstAndLastChar() = substring(1 until length - 1)

fun String.addBackticks() = "`$this`"
