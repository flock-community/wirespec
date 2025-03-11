package community.flock.wirespec.plugin.cli

import arrow.core.NonEmptySet
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.JSONFile
import community.flock.wirespec.plugin.files.JavaFile
import community.flock.wirespec.plugin.files.KotlinFile
import community.flock.wirespec.plugin.files.ScalaFile
import community.flock.wirespec.plugin.files.TypeScriptFile
import community.flock.wirespec.plugin.files.WirespecFile
import community.flock.wirespec.plugin.utils.orNull

fun main(args: Array<String>) {
    (0..20)
        .mapNotNull(args::orNull)
        .toTypedArray()
        .let(WirespecCli.provide(::compile, ::convert)::main)
}

fun FilePath.emitters(languages: NonEmptySet<Language>, packageName: PackageName) = languages.map {
    when (it) {
        Language.Java -> JavaEmitter(packageName) to JavaFile(this)
        Language.Kotlin -> KotlinEmitter(packageName) to KotlinFile(this)
        Language.Scala -> ScalaEmitter(packageName) to ScalaFile(this)
        Language.TypeScript -> TypeScriptEmitter() to TypeScriptFile(this)
        Language.Wirespec -> WirespecEmitter() to WirespecFile(this)
        Language.OpenAPIV2 -> OpenAPIV2Emitter to JSONFile(this)
        Language.OpenAPIV3 -> OpenAPIV3Emitter to JSONFile(this)
    }
}
