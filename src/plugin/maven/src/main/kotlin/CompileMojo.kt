package community.flock.wirespec.plugin.maven

import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.mapEmitter
import community.flock.wirespec.plugin.parse
import community.flock.wirespec.plugin.writeToFiles
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = "compile", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
open class CompileMojo : BaseMojo() {

    @Parameter
    protected var languages: List<Language>? = null

    @Parameter
    protected var shared: Boolean = true

    override fun execute() {
        val content = getFilesContent().parse(logger)
        languages
            ?.map { it.mapEmitter(packageNameValue, logger) }
            ?.forEach { (emitter, sharedData, ext) ->
                content.forEach { (fileName, ast) ->
                    emitter.emit(ast).forEach {
                        it.writeToFiles(
                            outputFile,
                            packageNameValue,
                            if (shared) sharedData else null,
                            fileName,
                            ext
                        )
                    }
                }
            }
    }
}

