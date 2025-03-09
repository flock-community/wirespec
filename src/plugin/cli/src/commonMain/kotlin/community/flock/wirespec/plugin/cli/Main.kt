package community.flock.wirespec.plugin.cli

import community.flock.wirespec.compiler.core.component1
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.plugin.DirectoryPath.Companion.toDirectoryPath
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Language.Java
import community.flock.wirespec.plugin.Language.Kotlin
import community.flock.wirespec.plugin.Language.OpenAPIV2
import community.flock.wirespec.plugin.Language.OpenAPIV3
import community.flock.wirespec.plugin.Language.Scala
import community.flock.wirespec.plugin.Language.TypeScript
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.Output
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.cli.io.File
import community.flock.wirespec.plugin.cli.io.JavaFile
import community.flock.wirespec.plugin.cli.io.JsonFile
import community.flock.wirespec.plugin.cli.io.KotlinFile
import community.flock.wirespec.plugin.cli.io.ScalaFile
import community.flock.wirespec.plugin.cli.io.TypeScriptFile
import community.flock.wirespec.plugin.cli.io.WirespecFile
import community.flock.wirespec.plugin.utils.orNull

fun main(args: Array<String>) {
    (0..20)
        .mapNotNull(args::orNull)
        .toTypedArray()
        .let(WirespecCli.provide(::compile, ::convert, ::write)::main)
}

fun Set<Language>.emitters(
    packageName: PackageName,
    path: ((FileExtension) -> FilePath),
) = map {
    val (packageString) = packageName
    when (it) {
        Java -> JavaEmitter(packageString) to JavaFile(path(FileExtension.Java))
        Kotlin -> KotlinEmitter(packageString) to KotlinFile(path(FileExtension.Kotlin))
        Scala -> ScalaEmitter(packageString) to ScalaFile(path(FileExtension.Scala))
        TypeScript -> TypeScriptEmitter() to TypeScriptFile(path(FileExtension.TypeScript))
        Wirespec -> WirespecEmitter() to WirespecFile(path(FileExtension.Wirespec))
        OpenAPIV2 -> OpenAPIV2Emitter to JsonFile(path(FileExtension.JSON))
        OpenAPIV3 -> OpenAPIV3Emitter to JsonFile(path(FileExtension.JSON))
    }
}

fun FilePath.out(packageName: PackageName, output: Output?) = { extension: FileExtension ->
    val dir = output ?: "$directory/out/${extension.name.lowercase()}"
    copy(
        directory = "$dir/${packageName.value.split('.').joinToString("/")}".toDirectoryPath(),
        extension = extension,
    )
}

fun write(file: File, output: List<Emitted>) = output.forEach { (name, result) ->
    file.copy(FileName(name)).write(result)
}
