package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.plugin.maven.Language.Java
import community.flock.wirespec.plugin.maven.Language.Kotlin
import community.flock.wirespec.plugin.maven.Language.Scala
import community.flock.wirespec.plugin.maven.Language.TypeScript
import community.flock.wirespec.plugin.maven.utils.JvmUtil
import java.io.File
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

enum class Language { Java, Kotlin, Scala, TypeScript }

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
            }
        } ?: compileDefault()
        project.addCompileSourceRoot(output)
    }

    private fun FilesContent.compileDefault() {
        compileKotlin()
        compileTypeScript()
    }

    private fun FilesContent.compileKotlin() {
        val emitter = KotlinEmitter(packageName, logger)
        if (shared) {
            JvmUtil.emitJvm("community.flock.wirespec", output, "Wirespec", "kt").writeText(emitter.shared)
        }
        compile(logger, emitter)
            .forEach { (name, result) -> JvmUtil.emitJvm(packageName, output, name, "kt").writeText(result) }
    }

    private fun FilesContent.compileJava() {
        val emitter = JavaEmitter(packageName, logger)
        if (shared) {
            JvmUtil.emitJvm("community.flock.wirespec", output, "Wirespec", "java").writeText(emitter.shared)
        }
        compile(logger, emitter)
            .forEach { (name, result) -> JvmUtil.emitJvm(packageName, output, name, "java").writeText(result) }
    }

    private fun FilesContent.compileScala() {
        val emitter = ScalaEmitter(packageName, logger)
        if (shared) {
            JvmUtil.emitJvm("community.flock.wirespec", output, "Wirespec", "scala").writeText(emitter.shared)
        }
        compile(logger, emitter)
            .forEach { (name, result) -> JvmUtil.emitJvm(packageName, output, name, "scala").writeText(result) }
    }

    private fun FilesContent.compileTypeScript() {
        val emitter = TypeScriptEmitter(logger)
        File(output).mkdirs()
        compile(logger, emitter)
            .forEach { (name, result) ->
                File("$output/$name.ts").writeText(result)
            }
    }
}
