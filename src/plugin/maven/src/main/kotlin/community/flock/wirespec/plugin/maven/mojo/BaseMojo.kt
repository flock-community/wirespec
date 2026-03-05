package community.flock.wirespec.plugin.maven.mojo

import arrow.core.NonEmptyList
import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.io.ClassPath
import community.flock.wirespec.plugin.io.Directory
import community.flock.wirespec.plugin.io.FilePath
import community.flock.wirespec.plugin.io.Name
import community.flock.wirespec.plugin.io.Source
import community.flock.wirespec.plugin.io.write
import community.flock.wirespec.plugin.maven.compiler.JavaCompiler
import community.flock.wirespec.plugin.maven.compiler.KotlinCompiler
import community.flock.wirespec.plugin.toEmitter
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File
import java.net.URLClassLoader

abstract class BaseMojo : AbstractMojo() {

    /**
     * Specifies the input files or directories.
     * Multiple paths can be provided, separated by commas.
     * Files can also be loaded from the classpath using the 'classpath:' prefix (e.g., 'classpath:wirespec/petstore.ws').
     */
    @Parameter(required = true)
    protected lateinit var input: String

    /**
     * Specifies the output directory.
     */
    @Parameter(required = true)
    protected lateinit var output: String

    /**
     * Specifies the languages to compile to: [Language].
     */
    @Parameter
    protected var languages: List<Language> = listOf()

    /**
     * Specifies whether to emit the shared Wirespec code.
     */
    @Parameter
    protected var shared: Boolean = true

    /**
     * Specifies what additional custom emitter to use.
     */
    @Parameter
    protected var emitterClass: String? = null

    /**
     * Specifies package name, default [DEFAULT_GENERATED_PACKAGE_STRING]
     */
    @Parameter
    protected var packageName: String = DEFAULT_GENERATED_PACKAGE_STRING

    /**
     * Specifies whether to invoke strict mode. Default 'true'.
     */
    @Parameter
    protected var strict: Boolean = true

    /**
     * Source directory. Default 'null'.
     */
    @Parameter
    protected var sourceDirectory: String? = null

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    protected lateinit var project: MavenProject

    protected val logger = object : Logger(ERROR) {
        override fun debug(string: String) = log.debug(string)
        override fun info(string: String) = log.info(string)
        override fun warn(string: String) = log.warn(string)
        override fun error(string: String) = log.error(string)
    }

    private fun emitter() = if (emitterClass != null) {
        val clazz = getClassLoader(project).loadClass(emitterClass) ?: error("No class found: $emitterClass")
        val constructor = clazz.constructors.first() ?: error("No constructor found: $emitterClass")
        val args: List<Any> = constructor.parameters
            .map {
                when (it.type) {
                    PackageName::class.java -> PackageName(packageName)
                    EmitShared::class.java -> EmitShared(shared)
                    else -> error("Cannot map constructor parameter: $emitterClass - ${it.type.simpleName}")
                }
            }
        constructor.newInstance(*args.toTypedArray()) as Emitter
    } else {
        null
    }

    val emitters
        get() = languages
            .map { it.toEmitter(PackageName(packageName), EmitShared(shared)) }
            .plus(emitter())
            .mapNotNull { it }
            .toNonEmptySetOrNull()
            ?: throw PickAtLeastOneLanguageOrEmitter()

    protected fun getClassLoader(project: MavenProject): ClassLoader = try {
        project.compileClasspathElements
            .apply {
                add(classOutputDir().absolutePath)
            }
            .map { File(it as String).toURI().toURL() }
            .toTypedArray()
            .let { URLClassLoader(it, javaClass.getClassLoader()) }
    } catch (_: Exception) {
        log.debug("Couldn't get the classloader.")
        javaClass.getClassLoader()
    }

    protected fun writer(directory: Directory): (NonEmptyList<Emitted>) -> Unit = { emittedList ->
        emittedList.forEach { emitted ->
            FilePath(directory.path.value + "/" + emitted.file).write(emitted.result)
        }
    }

    fun classOutputDir() = File(project.build.directory, "wirespec-classes")
        .apply { if (!exists()) mkdirs() }

    fun compileSourceDirectory() {
        if (sourceDirectory == null) return
        log.info("Compiling source directory: $sourceDirectory")
        project.addTestCompileSourceRoot(sourceDirectory)
        val file = File(sourceDirectory!!)
        KotlinCompiler(project, log, classOutputDir()).compile(file)
        JavaCompiler(project, log, classOutputDir()).compile(file)
    }

    protected inline fun <reified E : Source.Type> ClassPath.readFromClasspath(): Source<E> {
        val file = File(value)
        val classLoader = javaClass.classLoader
        val inputStream =
            classLoader.getResourceAsStream(value) ?: error("Could not find file: $value on the classpath.")
        val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val name = file.name.split(".").first()
        logger.info("Found 1 file from classpath: $file")
        return Source<E>(name = Name(name), content = content)
    }

    protected fun handleError(string: String): Nothing = throw RuntimeException(string)
}
