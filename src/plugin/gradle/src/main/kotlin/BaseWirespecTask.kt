package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.utils.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.options.Option

typealias FilesContent = List<Pair<String, String>>

abstract class BaseWirespecTask : DefaultTask() {

    @get:OutputDirectory
    @get:Option(option = "output", description = "output directory")
    abstract val output: DirectoryProperty

    @get:Input
    @get:Option(option = "packageName", description = "package name")
    abstract val packageName: Property<String>

    @Internal
    val wirespecLogger = object : Logger() {
        override fun debug(s: String) { logger.debug(s) }
        override fun info(s: String) { logger.info(s) }
        override fun warn(s: String) { logger.warn(s) }
    }

}