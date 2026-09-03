package community.flock.wirespec.plugin.gradle

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.extension.IrExtension
import community.flock.wirespec.ir.extension.applyExtensions
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.io.ClassPath
import community.flock.wirespec.plugin.io.Directory
import community.flock.wirespec.plugin.io.FilePath
import community.flock.wirespec.plugin.io.Name
import community.flock.wirespec.plugin.io.Source
import community.flock.wirespec.plugin.io.write
import community.flock.wirespec.plugin.toEmitter
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

public abstract class BaseWirespecTask : DefaultTask() {

    @get:OutputDirectory
    @get:Option(option = "output", description = "output directory")
    public abstract val output: DirectoryProperty

    @get:OutputDirectory
    @get:Optional
    @get:Option(option = "testOutput", description = "test output directory")
    public abstract val testOutput: DirectoryProperty

    @get:Input
    @get:Optional
    @get:Option(option = "languages", description = "languages list")
    public abstract val languages: ListProperty<Language>

    @get:Input
    @get:Optional
    @get:Option(option = "shared", description = "emit shared code")
    public abstract val shared: Property<Boolean>

    @get:Input
    @get:Optional
    @get:Option(option = "extensionClasses", description = "IR extension classes applied when an emitter is an IrEmitter")
    public abstract val extensionClasses: ListProperty<Class<*>>

    @get:Input
    @get:Optional
    @get:Option(option = "packageName", description = "package name")
    public abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "strict", description = "strict parsing mode")
    public abstract val strict: Property<Boolean>

    @Internal
    public val wirespecLogger: Logger = object : Logger(Level.INFO) {
        override fun debug(string: String) = logger.debug(string)
        override fun info(string: String) = logger.info(string)
        override fun warn(string: String) = logger.warn(string)
        override fun error(string: String) = logger.error(string)
    }

    protected fun packageNameValue(): PackageName = packageName.getOrElse(DEFAULT_GENERATED_PACKAGE_STRING).let(PackageName::invoke)
    protected fun sharedValue(): EmitShared = shared.getOrElse(false).let(EmitShared::invoke)

    protected fun extensionInstances(language: FileExtension): List<IrExtension> = extensionClasses.getOrElse(emptyList()).map { extensionClass ->
        try {
            val constructor = extensionClass.declaredConstructors.first()
            val args: List<Any> = constructor.parameters
                .map {
                    when (it.type) {
                        PackageName::class.java -> packageNameValue()
                        EmitShared::class.java -> sharedValue()
                        FileExtension::class.java -> language
                        else -> error("Cannot map constructor parameter")
                    }
                }
            constructor.newInstance(*args.toTypedArray()) as IrExtension
        } catch (e: Exception) {
            logger.error("Cannot create instance of extension: ${extensionClass.simpleName}", e)
            throw e
        }
    }

    protected fun emitters(): NonEmptySet<Emitter> = languages.get()
        .map { it.toEmitter(packageNameValue(), sharedValue()) }
        .map { it.applyExtensions(extensionInstances(it.extension)) }
        .toNonEmptySetOrNull()
        ?: throw PickAtLeastOneLanguage()

    protected fun writer(directory: Directory, testDirectory: Directory? = null): (NonEmptyList<Emitted>) -> Unit = { emittedList ->
        emittedList.forEach { emitted ->
            val target = if (emitted.test && testDirectory != null) testDirectory else directory
            FilePath(target.path.value + "/" + emitted.file).write(emitted.result)
        }
    }

    public inline fun <reified E : Source.Type> ClassPath.readFromClasspath(preProcess: ((String) -> String)): Source<E> {
        val file = File(value)
        val classLoader = javaClass.classLoader
        val inputStream = classLoader
            .getResourceAsStream(value) ?: error("Could not find file: $value on the classpath.")
        val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val name = file.name.split(".").first()
        logger.info("Found 1 file from classpath: $file")
        return Source(name = Name(name), content = preProcess(content))
    }

    protected fun handleError(string: String): Nothing = throw RuntimeException(string)
}
