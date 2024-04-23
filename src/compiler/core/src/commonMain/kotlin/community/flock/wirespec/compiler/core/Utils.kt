package community.flock.wirespec.compiler.core

fun String.hasBackticks() = length > 1 && startsWith('`') && endsWith('`')

fun String.removeBackticks() = if (hasBackticks()) substring(1 until length - 1) else this

fun String.addBackticks() = "`$this`"
