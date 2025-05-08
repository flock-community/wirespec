package community.flock.wirespec.plugin.maven.community.flock.wirespec.plugin.maven.mojo

import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.PythonEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.EmitShared
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
import community.flock.wirespec.plugin.maven.compiler.JavaCompiler
import community.flock.wirespec.plugin.maven.compiler.KotlinCompiler
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

    private val emitter
        get() = try {
            val clazz = getClassLoader(project).loadClass(emitterClass)
            val constructor = clazz.constructors.first()
            val args: List<Any> = constructor.parameters
                .map {
                    when (it.type) {
                        PackageName::class.java -> PackageName(packageName)
                        EmitShared::class.java -> EmitShared(shared)
                        else -> error("Cannot map constructor parameter")
                    }
                }
            constructor.newInstance(*args.toTypedArray()) as Emitter
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    val emitters
        get() = languages.map {
            when (it) {
                Language.Java -> JavaEmitter(PackageName(packageName), EmitShared(shared))
                Language.Kotlin -> KotlinEmitter(PackageName(packageName), EmitShared(shared))
                Language.Python -> PythonEmitter(PackageName(packageName), EmitShared(shared))
                Language.TypeScript -> TypeScriptEmitter()
                Language.Wirespec -> WirespecEmitter()
                Language.OpenAPIV2 -> OpenAPIV2Emitter
                Language.OpenAPIV3 -> OpenAPIV3Emitter
            }
        }.plus(emitter)
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
