package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.cli.Language.Kotlin
import community.flock.wirespec.compiler.cli.Language.TypeScript
import community.flock.wirespec.compiler.cli.io.AbstractFile
import community.flock.wirespec.compiler.cli.io.KotlinFile
import community.flock.wirespec.compiler.cli.io.TypeScriptFile
import community.flock.wirespec.compiler.cli.io.WireSpecFile
import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.getOrHandle
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.getEnvVar
import community.flock.wirespec.compiler.utils.getFirst
import community.flock.wirespec.compiler.utils.getSecond

const val typesDir = "/types"

private enum class Language { Kotlin, TypeScript }

private val enableLogging = getEnvVar("WIRE_SPEC_LOGGING_ENABLED").toBoolean()

private val logger = object : Logger(enableLogging) {}

private fun Logger.from(s: String): Language? = runCatching { Language.valueOf(s) }.getOrNull().also {
    if (it == null) warn("'$s' is not known to WireSpec. Choose from ${Language.values().joinToString(",")}")
}

fun main(args: Array<String>) {


    val basePath = getFirst(args)
    val languages = getSecond(args)
        ?.split(",")
        ?.mapNotNull(logger::from)
        ?.toSet()
        ?: setOf(Kotlin)

    val typesPath = "$basePath$typesDir"
    val inputPath = "$typesPath/in/input"
    val outputPath = "$typesPath/out/output"

    languages.forEach {
        when (it) {
            Kotlin -> KotlinEmitter(logger) to KotlinFile(outputPath)
            TypeScript -> TypeScriptEmitter(logger) to TypeScriptFile(outputPath)
        }.let(compile(inputPath))
    }

}

fun compile(inputPath: String): (Pair<Emitter, AbstractFile>) -> Unit = { (emitter, file) ->
    WireSpecFile(inputPath).read()
        .let(WireSpec::compile)(logger)(emitter)
        .getOrHandle { throw it }
        .let(file::write)
}
