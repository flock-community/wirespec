package community.flock.wirespec.plugin.maven

import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
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
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.FullPath
import community.flock.wirespec.plugin.files.Name
import community.flock.wirespec.plugin.files.Source
import community.flock.wirespec.plugin.files.SourcePath
import community.flock.wirespec.plugin.files.path
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
     * Specifies the output directories to process.
     */
    @Parameter(required = true)
    protected lateinit var output: String

    @Parameter
    protected var languages: List<Language> = listOf()

    @Parameter
    protected var shared: Boolean = true

    @Parameter
    protected var emitterClass: String? = null

    /**
     * Specifies package name default 'community.flock.wirespec.generated'
     */
    @Parameter
    protected var packageName: String = DEFAULT_GENERATED_PACKAGE_STRING

    @Parameter
    protected var strict: Boolean = true

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    protected lateinit var project: MavenProject

    protected val logger = object : Logger(ERROR) {
        override fun debug(string: String) = log.debug(string)
        override fun info(string: String) = log.info(string)
        override fun warn(string: String) = log.warn(string)
        override fun error(string: String) = log.error(string)
    }

    fun getFullPath(input: String?, createIfNotExists: Boolean = false) = when {
        input == null -> null
        input.startsWith("classpath:") -> SourcePath(input.substringAfter("classpath:"))
        else -> {
            val file = File(input).createIfNotExists(createIfNotExists)
            when {
                file.isDirectory -> DirectoryPath(file.absolutePath)
                file.isFile -> FilePath(file.absolutePath)
                else -> throw IsNotAFileOrDirectory(input)
            }
        }
    }

    fun getOutPutPath(inputPath: FullPath) = when (val it = getFullPath(output, true)) {
        null -> DirectoryPath("${inputPath.path()}/out")
        is DirectoryPath -> it
        is FilePath, is SourcePath -> throw OutputShouldBeADirectory()
    }

    private val emitter
        get() = try {
            val clazz = getClassLoader(project).loadClass(emitterClass)
            val constructor = clazz.constructors.first()
            val args: List<Any> = constructor.parameters
                .map {
                    when (it.type) {
                        String::class.java -> packageName
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
                Language.Java -> JavaEmitter(PackageName(packageName))
                Language.Kotlin -> KotlinEmitter(PackageName(packageName))
                Language.Scala -> ScalaEmitter(PackageName(packageName))
                Language.TypeScript -> TypeScriptEmitter()
                Language.Wirespec -> WirespecEmitter()
                Language.OpenAPIV2 -> OpenAPIV2Emitter
                Language.OpenAPIV3 -> OpenAPIV3Emitter
            }
        }.plus(emitter)
            .mapNotNull { it }
            .toNonEmptySetOrNull()
            ?: throw PickAtLeastOneLanguageOrEmitter()

    private fun getClassLoader(project: MavenProject): ClassLoader = try {
        project.compileClasspathElements
            .apply {
                add(project.build.outputDirectory)
                add(project.build.testOutputDirectory)
            }
            .map { File(it as String).toURI().toURL() }
            .toTypedArray()
            .let { URLClassLoader(it, javaClass.getClassLoader()) }
    } catch (_: Exception) {
        log.debug("Couldn't get the classloader.")
        javaClass.getClassLoader()
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
}
