package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.options.Option

abstract class BaseWirespecTask : DefaultTask() {

    @get:OutputDirectory
    @get:Option(option = "output", description = "output directory")
    abstract val output: DirectoryProperty

    @get:Input
    @get:Option(option = "packageName", description = "package name")
    abstract val packageName: Property<String>

    @Internal
    val wirespecLogger = object : Logger(ERROR) {
        override fun debug(string: String) = logger.debug(string)
        override fun info(string: String) = logger.info(string)
        override fun warn(string: String) = logger.warn(string)
        override fun error(string: String) = logger.error(string)
    }

}
