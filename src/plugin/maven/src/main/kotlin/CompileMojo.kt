package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Language.Java
import community.flock.wirespec.plugin.Language.Kotlin
import community.flock.wirespec.plugin.Language.Scala
import community.flock.wirespec.plugin.Language.TypeScript
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.maven.utils.JvmUtil
import java.io.File
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
        getFilesContent().compileLanguages()
    }

    private fun FilesContent.compileLanguages() {
        languages?.forEach {
            when (it) {
                Java -> compileJava()
                Kotlin -> compileKotlin()
                Scala -> compileScala()
                TypeScript -> compileTypeScript()
                Wirespec -> TODO()
            }
        } ?: compileDefault()
        project.addCompileSourceRoot(output)
    }

    private fun FilesContent.compileDefault() {
        compileKotlin()
        compileTypeScript()
    }

    private fun FilesContent.compileKotlin() {
        KotlinEmitter(packageName, logger)
            .apply { compile(logger, this).writeToJvmFiles(FileExtension.Kotlin, shared) }
    }

    private fun FilesContent.compileJava() {
        JavaEmitter(packageName, logger)
            .apply { compile(logger, this).writeToJvmFiles(FileExtension.Java, shared) }
    }

    private fun FilesContent.compileScala() {
        ScalaEmitter(packageName, logger)
            .apply { compile(logger, this).writeToJvmFiles(FileExtension.Scala, shared) }
    }

    private fun FilesContent.compileTypeScript() {
        compile(logger, TypeScriptEmitter(logger)).writeToFiles(FileExtension.TypeScript)
    }

    protected fun List<Emitted>.writeToJvmFiles(fe: FileExtension, sharedSource: String, fileName: String? = null) =
        forEach { (name, result) ->
            JvmUtil.emitJvm(packageName, output, fileName ?: name, fe.value).writeText(result)
            if (shared) JvmUtil
                .emitJvm("community.flock.wirespec", output, Wirespec.name, fe.value)
                .writeText(sharedSource)
        }

    protected fun List<Emitted>.writeToFiles(fe: FileExtension, filename: String? = null) = forEach { (name, result) ->
        File(output).mkdirs()
        File("$output/${filename ?: name}.${fe.value}").writeText(result)
    }
}
