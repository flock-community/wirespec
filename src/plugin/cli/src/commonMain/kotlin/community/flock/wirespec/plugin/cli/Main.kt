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
import community.flock.wirespec.plugin.Directory
import community.flock.wirespec.plugin.File
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileExtension.JSON
import community.flock.wirespec.plugin.FileExtension.Java
import community.flock.wirespec.plugin.FileExtension.Kotlin
import community.flock.wirespec.plugin.FileExtension.Scala
import community.flock.wirespec.plugin.FileExtension.TypeScript
import community.flock.wirespec.plugin.FileExtension.Wirespec
import community.flock.wirespec.plugin.FilePath
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.files.JavaFile
import community.flock.wirespec.plugin.files.JsonFile
import community.flock.wirespec.plugin.files.KotlinFile
import community.flock.wirespec.plugin.files.ScalaFile
import community.flock.wirespec.plugin.files.TypeScriptFile
import community.flock.wirespec.plugin.files.WirespecFile
import community.flock.wirespec.plugin.plus
import community.flock.wirespec.plugin.utils.orNull

fun main(args: Array<String>) {
    (0..20)
        .mapNotNull(args::orNull)
        .toTypedArray()
        .let(WirespecCli.provide(::compile, ::convert)::main)
}

fun NonEmptySet<Language>.emitters(
    packageName: PackageName,
    path: ((FileExtension) -> FilePath),
) = map {
    when (it) {
        Language.Java -> JavaEmitter(packageName) to JavaFile(path(Java))
        Language.Kotlin -> KotlinEmitter(packageName) to KotlinFile(path(Kotlin))
        Language.Scala -> ScalaEmitter(packageName) to ScalaFile(path(Scala))
        Language.TypeScript -> TypeScriptEmitter() to TypeScriptFile(path(TypeScript))
        Language.Wirespec -> WirespecEmitter() to WirespecFile(path(Wirespec))
        Language.OpenAPIV2 -> OpenAPIV2Emitter to JsonFile(path(JSON))
        Language.OpenAPIV3 -> OpenAPIV3Emitter to JsonFile(path(JSON))
    }
}

fun File.out(packageName: PackageName, output: Directory) = { extension: FileExtension ->
    path.copy(
        extension = extension,
        directory = output.path + when (packageName.createDirectory) {
            true -> "/${packageName.value.split('.').joinToString("/")}"
            false -> ""
        },
    )
}
