package community.flock.wirespec.plugin.gradle

import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.PythonEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.io.Name
import community.flock.wirespec.plugin.io.Source
import community.flock.wirespec.plugin.io.SourcePath
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.options.Option
import java.io.File

abstract class BaseWirespecTask : DefaultTask() {

    @get:OutputDirectory
    @get:Option(option = "output", description = "output directory")
    abstract val output: DirectoryProperty

    @get:Input
    @get:Optional
    @get:Option(option = "languages", description = "languages list")
    abstract val languages: ListProperty<Language>

    @get:Input
    @get:Optional
    @get:Option(option = "shared", description = "emit shared code")
    abstract val shared: Property<Boolean>

    @get:Input
    @get:Optional
    @get:Option(option = "emitterClass", description = "custom emitter class")
    abstract val emitterClass: Property<Class<*>>

    @get:Input
    @get:Optional
    @get:Option(option = "packageName", description = "package name")
    abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "strict", description = "strict parsing mode")
    abstract val strict: Property<Boolean>

    @Internal
    val wirespecLogger = object : Logger(ERROR) {
        override fun debug(string: String) = logger.debug(string)
        override fun info(string: String) = logger.info(string)
        override fun warn(string: String) = logger.warn(string)
        override fun error(string: String) = logger.error(string)
    }

    protected fun packageNameValue() = packageName.getOrElse(DEFAULT_GENERATED_PACKAGE_STRING).let(PackageName::invoke)

    protected fun emitter() = try {
        emitterClass.orNull?.getDeclaredConstructor()?.newInstance() as? Emitter
    } catch (e: Exception) {
        logger.error("Cannot create instance of emitter: ${emitterClass.orNull?.simpleName}", e)
        throw e
    }

    protected fun emitters() = languages.get().map {
        when (it) {
            Language.Java -> JavaEmitter(packageNameValue(), sharedValue())
            Language.Kotlin -> KotlinEmitter(packageNameValue(),sharedValue())
            Language.Python -> PythonEmitter(packageNameValue(),sharedValue())
            Language.TypeScript -> TypeScriptEmitter(sharedValue())
            Language.Wirespec -> WirespecEmitter()
            Language.OpenAPIV2 -> OpenAPIV2Emitter
            Language.OpenAPIV3 -> OpenAPIV3Emitter
        }
    }.plus(emitter())
        .mapNotNull { it }
        .toNonEmptySetOrNull()
        ?: throw PickAtLeastOneLanguageOrEmitter()

    inline fun <reified E : Source.Type> SourcePath.readFromClasspath(): Source<E> {
        val file = File(value)
        val classLoader = javaClass.classLoader
        val inputStream =
            classLoader.getResourceAsStream(value) ?: error("Could not find file: $value on the classpath.")
        val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val name = file.name.split(".").first()
        return Source<E>(name = Name(name), content = content)
    }

    protected fun handleError(string: String): Nothing = throw RuntimeException(string)
}
