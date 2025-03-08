package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.plugin.FileContent
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.mapEmitter
import community.flock.wirespec.plugin.parse
import community.flock.wirespec.plugin.writeToFiles
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class CompileWirespecTask : BaseWirespecTask() {

    @get:InputDirectory
    @get:Option(option = "input", description = "input directory")
    abstract val input: DirectoryProperty

    @get:Input
    @get:Option(option = "languages", description = "languages list")
    abstract val languages: ListProperty<Language>

    @get:Optional
    @get:Input
    @get:Option(option = "shared", description = "emit shared class")
    abstract val shared: Property<Boolean>

    @Internal
    protected fun getFilesContent(): List<FileContent> = input.asFileTree
        .map { it.name.split(".").first() to it.readText(Charsets.UTF_8) }
        .map(::FileContent)

    @TaskAction
    fun action() {
        val packageNameValue = packageName.map { PackageName(it) }.get()
        val ast = getFilesContent().parse(wirespecLogger)
        languages.get()
            .map { it.mapEmitter(packageNameValue, wirespecLogger) }
            .forEach { (emitter, ext, sharedData) ->
                ast.forEach { (fileName, ast) ->
                    emitter.emit(ast).writeToFiles(
                        output = output.asFile.get(),
                        packageName = packageNameValue,
                        shared = if (shared.getOrElse(true)) sharedData else null,
                        fileName = if (emitter.split) null else fileName,
                        ext = ext,
                    )
                }
            }
    }
}
