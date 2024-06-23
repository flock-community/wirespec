package community.flock.wirespec.plugin.gradle

import arrow.core.Either
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.PackageName
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.options.Option

typealias FilesContent = List<Pair<String, String>>

abstract class BaseWirespecTask: DefaultTask() {

    @get:InputDirectory
    @get:Option(option = "input", description = "input directory")
    abstract val input: DirectoryProperty

    @get:OutputDirectory
    @get:Option(option = "output", description = "output directory")
    abstract val output: DirectoryProperty

    @get:Input
    @get:Option(option = "packageName", description = "package name")
    abstract val packageName: Property<String>

    @Internal
    val wirespecLogger = object : Logger() {
        override fun log(s: String) { logger.info(s) }
        override fun warn(s: String) { logger.warn(s) }
    }

    @Internal
    protected fun getFilesContent(): FilesContent = input.asFileTree
        .map { it.name.split(".").first() to it.readText(Charsets.UTF_8) }
        .map { (name, reader) -> name to reader }

}