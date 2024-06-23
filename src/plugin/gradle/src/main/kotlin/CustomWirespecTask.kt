package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.toDirectory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class CustomWirespecTask : BaseWirespecTask() {

    @get:Input
    @get:Option(option = "emitter", description = "emitter")
    abstract val emitter: Property<Class<*>>

    @get:Optional
    @get:Input
    @get:Option(option = "shared", description = "shared")
    abstract val shared: Property<String>

    @get:Optional
    @get:Input
    @get:Option(option = "shared", description = "shared")
    abstract val extension: Property<String>

    @TaskAction
    fun custom() {
        val ext = extension.get()
        val defaultPkg = PackageName("community.flock.wirespec")
        val emitterPkg = packageName.map { PackageName(it) }.getOrElse(defaultPkg)
        val emitter: Emitter = try {
            emitter
                .map { it.getDeclaredConstructor().newInstance() }
                .get() as Emitter
        } catch (e: Exception) {
            logger.error("Cannot create instance of emitter: ${emitter.get().simpleName}", e)
            throw e
        }

        getFilesContent()
            .compile(wirespecLogger, emitter)
            .also {
                output.dir(defaultPkg.toDirectory()).get().asFile.apply {
                    mkdirs()
                    resolve("Wirespec.$ext").writeText(shared.get())
                }
            }
            .onEach { (name, result) ->
                output.dir(emitterPkg.toDirectory()).get().asFile.apply {
                    mkdirs()
                    resolve("${name}.$ext").writeText(result)
                }
            }
    }
}
