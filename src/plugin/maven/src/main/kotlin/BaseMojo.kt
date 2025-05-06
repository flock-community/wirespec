package community.flock.wirespec.plugin.maven

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
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File
import java.io.IOException
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.*
import java.util.Set
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.StandardJavaFileManager
import javax.tools.StandardLocation
import javax.tools.ToolProvider


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

    /**
     * Compiles a Java or Kotlin source file using the Java Compiler API.
     * @param javaFile Location of the java file to compile.
     * @return True if compilation was successful, false otherwise
     */
    protected fun compileJavaClass(javaFile: PreProcessor.JavaFile): List<String> {
        val sourceFilePath = File(javaFile.filePath)

        if (!sourceFilePath.exists()) {
            log.info("Source file not found: " + sourceFilePath);
            throw MojoFailureException("Source file " + sourceFilePath + " does not exist.");
        }

        val compiler = ToolProvider.getSystemJavaCompiler()
        if (compiler == null) {
            throw MojoExecutionException("Could not get system Java compiler. Ensure you are running Maven with a JDK, not just a JRE.")
        }

        val fileManager = compiler.getStandardFileManager(
            null,
            null,
            StandardCharsets.UTF_8
        ).apply {
            val outputDir = File(project.build.outputDirectory)
            outputDir.mkdirs()
            log.info("Output directory: $outputDir")
            setLocation(StandardLocation.CLASS_OUTPUT, listOf(outputDir))
        }

        val compilationUnits = fileManager.getJavaFileObjects(sourceFilePath)
        val options = buildCompilerOptions(fileManager)
        val diagnostics = DiagnosticCollector<JavaFileObject?>()
        val task = compiler.getTask(
            null,  // Output writer
            fileManager,
            diagnostics,
            options,
            null,  // Annotation processor class names
            compilationUnits
        )

        val success =  task.call()

        log.info("Compilation result: " + success)
        val outputLocation = StandardLocation.CLASS_OUTPUT

        if (success) {
            fileManager.flush()
            log.info("Compiled classes:" + fileManager.list(outputLocation, "", mutableSetOf(JavaFileObject.Kind.SOURCE), true))
            log.info("Compiled classes:" + fileManager.list(outputLocation, "", mutableSetOf(JavaFileObject.Kind.CLASS), true))
            try {
                // Assuming standard output location. This might vary based on fileManager configuration.

                val outputFiles = fileManager.list(outputLocation, "", mutableSetOf(JavaFileObject.Kind.CLASS), true)
                log.info("outputFiles: $outputFiles")
                // Get the base output path to correctly relativize class file paths
                val outputDirectory: Path = Path.of(fileManager.getLocation(outputLocation).iterator().next().toURI())

                val compiledClassNames = outputFiles
                    .filter { fileObject -> fileObject.kind == JavaFileObject.Kind.CLASS}
                    .map { fileObject ->
                        val filePath: Path? = Path.of(fileObject.toUri())
                        val relativePath: Path = outputDirectory.relativize(filePath)
                        val className = relativePath.toString()
                            .replace(FileSystems.getDefault().separator, ".") // Use system-specific separator
                            .replace(".class", "")
                        println("className: $className")
                        className
                    }
                return compiledClassNames
            } catch (e: IOException) {
                throw MojoFailureException("Error accessing compiled files: ", e)
            }
        } else {
            throw MojoFailureException("Compilation failed.")
        }
    }

    @Throws(MojoExecutionException::class)
    private fun buildCompilerOptions(fileManager: StandardJavaFileManager?): MutableList<String?> {
        val outputDir = File(project.build.outputDirectory)
        val options: MutableList<String?> = ArrayList<String?>()

        log.info("outputDir: " + outputDir)

        // Classpath (ensure classpathElements is initialized, e.g., via @Parameter)
        if (project.compileClasspathElements != null && project.compileClasspathElements.isNotEmpty()) {
            val classpath = project.compileClasspathElements.joinToString(File.pathSeparator)
            options.addAll(listOf("-classpath", classpath))
            log.debug("Classpath: $classpath")
        } else {
            log.warn("No compile classpath elements found.")
        }


        // Output directory
        options.addAll(listOf("-d", outputDir.absolutePath))

//        // Source/Target versions
//        if (sourceVersion != null) {
//            options.addAll(Arrays.asList("-source", sourceVersion))
//        }
//        if (targetVersion != null) {
//            options.addAll(Arrays.asList("-target", targetVersion))
//        }
//
//        // Encoding
//        if (sourceEncoding != null) {
//            options.addAll(Arrays.asList("-encoding", sourceEncoding))
//        }

        return options
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
