package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.plugin.FileContent
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.toDirectory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class CustomWirespecTask : BaseWirespecTask() {

    @get:Input
    @get:Optional
    @get:Option(option = "extension", description = "file extension")
    abstract val extension: Property<String>

    @Internal
    protected fun getFilesContent(): List<FileContent> = input.asFileTree
        .map { it.name.split(".").first() to it.readText(Charsets.UTF_8) }
        .map(::FileContent)

    @TaskAction
    fun custom() {
        val defaultPkg = PackageName("community.flock.wirespec")
        val emitterPkg = packageName.map { PackageName(it) }.getOrElse(defaultPkg)
        val emitter: Emitter = try {
            emitter.orNull?.getDeclaredConstructor()?.newInstance() as Emitter
        } catch (e: Exception) {
            logger.error("Cannot create instance of emitter: ${emitter.orNull?.simpleName}", e)
            throw e
        }
        val ext = extension.get()

        getFilesContent()
            .compile(wirespecLogger, emitter)
            .also {
                output.dir(PackageName(emitter.shared?.packageString).toDirectory()).get().asFile.apply {
                    mkdirs()
                    emitter.shared?.source?.let { resolve("Wirespec.$ext").writeText(it) }
                }
            }
            .onEach { (name, result) ->
                output.dir(emitterPkg.toDirectory()).get().asFile.apply {
                    mkdirs()
                    resolve("$name.$ext").writeText(result)
                }
            }
    }
}
