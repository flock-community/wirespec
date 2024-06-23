package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.mapEmitter
import community.flock.wirespec.plugin.parse
import community.flock.wirespec.plugin.writeToFiles
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class CompileWirespecTask : BaseWirespecTask() {

    @get:Input
    @get:Option(option = "languages", description = "languages list")
    abstract val languages: ListProperty<Language>

    @get:Optional
    @get:Input
    @get:Option(option = "shared", description = "emit shared class")
    abstract val shared: Property<Boolean>

    @TaskAction
    fun action() {
        val packageNameValue = packageName.map { PackageName(it) }.get()
        val ast = getFilesContent().parse(wirespecLogger)
        languages.get()
            .map { it.mapEmitter(packageNameValue, wirespecLogger) }
            .forEach { (emitter, sharedData, ext) ->
                ast.forEach { (fileName, ast) ->
                    println("-------")
                    println(fileName)
                    println("-------")
                    emitter.emit(ast).forEach {
                        it.writeToFiles(
                            output.asFile.get(),
                            packageNameValue,
                            if (shared.getOrElse(true)) sharedData else null,
                            fileName,
                            ext
                        )
                    }
                }
            }
    }
}
