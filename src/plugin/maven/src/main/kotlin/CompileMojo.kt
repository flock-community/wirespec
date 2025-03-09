package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.mapEmitter
import community.flock.wirespec.plugin.parse
import community.flock.wirespec.plugin.writeToFiles
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@Mojo(name = "compile", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
open class CompileMojo : BaseMojo() {

    @Parameter
    protected var languages: List<Language>? = null

    @Parameter
    protected var shared: Boolean = true

    override fun execute() {
        project.addCompileSourceRoot(output)
        val outputDirectory = File(output)
        val packageNameValue = PackageName(packageName)
        val asts = getFilesContent().parse(logger)

        emit(packageNameValue, asts, outputDirectory)
    }

    protected fun emit(
        packageNameValue: PackageName,
        asts: List<Pair<String, AST>>,
        outputFile: File,
    ) {
        languages
            ?.map { it.mapEmitter(packageNameValue) }
            ?.forEach { (emitter, ext, sharedData) ->
                asts.forEach { (fileName, ast) ->
                    emitter.emit(ast, logger).writeToFiles(
                        output = outputFile,
                        packageName = packageNameValue,
                        shared = if (shared) sharedData else null,
                        fileName = if (emitter.split) null else fileName,
                        ext = ext,
                    )
                }
            }
    }
}
