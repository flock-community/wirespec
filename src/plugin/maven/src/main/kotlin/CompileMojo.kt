package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.shared.JavaShared
import community.flock.wirespec.compiler.core.emit.shared.KotlinShared
import community.flock.wirespec.compiler.core.emit.shared.ScalaShared
import community.flock.wirespec.compiler.core.emit.shared.Shared
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Language.Java
import community.flock.wirespec.plugin.Language.Kotlin
import community.flock.wirespec.plugin.Language.Scala
import community.flock.wirespec.plugin.Language.TypeScript
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.toDirectory
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
        KotlinEmitter(packageName, decorators, logger)
            .apply { compile(logger, this).writeToJvmFiles(FileExtension.Kotlin, KotlinShared) }
    }

    private fun FilesContent.compileJava() {
        JavaEmitter(packageName, decorators, logger)
            .apply { compile(logger, this).writeToJvmFiles(FileExtension.Java, JavaShared) }
    }

    private fun FilesContent.compileScala() {
        ScalaEmitter(packageName, logger)
            .apply { compile(logger, this).writeToJvmFiles(FileExtension.Scala, ScalaShared) }
    }

    private fun FilesContent.compileTypeScript() {
        compile(logger, TypeScriptEmitter(logger)).writeToFiles(FileExtension.TypeScript)
    }

    protected fun List<Emitted>.writeToFiles(ext: FileExtension, filename: String? = null) = forEach { (name, result) ->
        File(output).mkdirs()
        File("$output/${filename ?: name}.${ext.value}").writeText(result)
    }

    protected fun List<Emitted>.writeToJvmFiles(ext: FileExtension, shared: Shared, fileName: String? = null) =
        forEach { (name, result) ->
            writeJvmFile(output, PackageName(packageName), fileName ?: name, ext).writeText(result)
            if (this@CompileMojo.shared)
                writeJvmFile(output, PackageName("community.flock.wirespec"), Wirespec.name, ext)
                    .writeText(shared.source)
        }

    private fun writeJvmFile(output: String, packageName: PackageName, name: String, ext: FileExtension) =
        "$output${packageName.toDirectory()}"
            .also { File(it).mkdirs() }
            .let { File("$it/$name.${ext.value}") }


}
