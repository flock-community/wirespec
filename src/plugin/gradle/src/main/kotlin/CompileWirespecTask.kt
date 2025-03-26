package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.plugin.FileContent
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.mapEmitter
import community.flock.wirespec.plugin.parse
import community.flock.wirespec.plugin.toDirectory
import community.flock.wirespec.plugin.writeToFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class CompileWirespecTask : BaseWirespecTask() {

    @Internal
    protected fun getFilesContent(): List<FileContent> = input.asFileTree
        .map { it.name.split(".").first() to it.readText(Charsets.UTF_8) }
        .map(::FileContent)

    @TaskAction
    fun action() {
        val packageNameValue = packageName.getOrElse("community.flock.wirespec").let(PackageName::invoke)
        try {
            emitter.orNull?.getDeclaredConstructor()?.newInstance() as? Emitter
        } catch (e: Exception) {
            logger.error("Cannot create instance of emitter: ${emitter.orNull?.simpleName}", e)
            throw e
        }?.let { emitter ->
            val ext = emitter.extension.value
            getFilesContent()
                .compile(wirespecLogger, emitter)
                .also {
                    output.dir(PackageName(emitter.shared?.packageString).toDirectory()).get().asFile.apply {
                        mkdirs()
                        emitter.shared?.source?.let { resolve("Wirespec.$ext").writeText(it) }
                    }
                }
                .onEach { (name, result) ->
                    output.dir(packageNameValue.toDirectory()).get().asFile.apply {
                        mkdirs()
                        resolve("$name.$ext").writeText(result)
                    }
                }
        }

        val ast = getFilesContent().parse(wirespecLogger)

        languages.get()
            .map { it.mapEmitter(packageNameValue) }
            .forEach { (emitter, ext, sharedData) ->
                ast.forEach { (fileName, ast) ->
                    emitter.emit(ast, wirespecLogger).writeToFiles(
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
