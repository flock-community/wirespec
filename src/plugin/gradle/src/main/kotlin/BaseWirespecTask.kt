package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.plugin.Language
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.options.Option

abstract class BaseWirespecTask : DefaultTask() {

    @get:InputDirectory
    @get:Option(option = "input", description = "input directory")
    abstract val input: DirectoryProperty

    @get:OutputDirectory
    @get:Option(option = "output", description = "output directory")
    abstract val output: DirectoryProperty

    @get:Input
    @get:Option(option = "languages", description = "languages list")
    abstract val languages: ListProperty<Language>

    @get:Input
    @get:Optional
    @get:Option(option = "shared", description = "emit shared code")
    abstract val shared: Property<Boolean>

    @get:Input
    @get:Optional
    @get:Option(option = "emitter", description = "emitter")
    abstract val emitter: Property<Class<*>>

    @get:Input
    @get:Optional
    @get:Option(option = "packageName", description = "package name")
    abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "strict", description = "strict parsing mode")
    abstract val strict: Property<Boolean>

    @Internal
    val wirespecLogger = object : Logger(ERROR) {
        override fun debug(string: String) = logger.debug(string)
        override fun info(string: String) = logger.info(string)
        override fun warn(string: String) = logger.warn(string)
        override fun error(string: String) = logger.error(string)
    }
}
