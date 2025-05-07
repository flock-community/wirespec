package community.flock.wirespec.plugin.maven.community.flock.wirespec.plugin.maven.mojo

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.common.FileExtension.Avro
import community.flock.wirespec.compiler.core.emit.common.FileExtension.JSON
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.convert
import community.flock.wirespec.plugin.io.Directory
import community.flock.wirespec.plugin.io.DirectoryPath
import community.flock.wirespec.plugin.io.FilePath
import community.flock.wirespec.plugin.io.Source
import community.flock.wirespec.plugin.io.Source.Type.JSON
import community.flock.wirespec.plugin.io.SourcePath
import community.flock.wirespec.plugin.io.getFullPath
import community.flock.wirespec.plugin.io.getOutPutPath
import community.flock.wirespec.plugin.io.or
import community.flock.wirespec.plugin.io.read
import community.flock.wirespec.plugin.io.write
import community.flock.wirespec.plugin.maven.compiler.JavaCompiler
import community.flock.wirespec.plugin.maven.compiler.KotlinCompiler
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import java.lang.reflect.Modifier
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Path
import javax.tools.JavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider


sealed interface PreProcessor {
    sealed interface File
    data class KotlinFile(val filePath: String) : PreProcessor, File
    data class JavaFile(val filePath: String) : PreProcessor, File
    data class ClassName(val className: String) : PreProcessor
}

@Suppress("unused")
@Mojo(
    name = "convert",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
)
class ConvertMojo : BaseMojo() {

    /**
     * Specifies the format to convert from: [Format].
     */
    @Parameter(required = true)
    private lateinit var format: Format

    /**
     * Specifies a preprocessor class that implements a function from String to String.
     * This class must be available in the project's classpath.
     */
    @Parameter
    private var preProcessor: String? = null

    private fun compileClas(file: PreProcessor.File) {
        when (file) {
            is PreProcessor.KotlinFile -> KotlinCompiler(project, log, classOutputDir()).compile(file)
            is PreProcessor.JavaFile -> JavaCompiler(project, log, classOutputDir()).compile(file)
        }
    }

    private fun loadClass(className: String): Class<*> {
        val classLoader = getClassLoader(project)
        return classLoader.loadClass(className)
    }

    private fun loadCompiledClasses(): List<String> {
        val compiler = ToolProvider.getSystemJavaCompiler()
        if (compiler == null) {
            throw MojoExecutionException("Could not get system Java compiler. Ensure you are running Maven with a JDK, not just a JRE.")
        }

        val fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)
            .apply { setLocation(StandardLocation.CLASS_OUTPUT, listOf(classOutputDir())) }

        val outputLocation = StandardLocation.CLASS_OUTPUT
        val outputFiles = fileManager.list(outputLocation, "", mutableSetOf(JavaFileObject.Kind.CLASS), true)
        val outputDirectory: Path = Path.of(fileManager.getLocation(outputLocation).iterator().next().toURI())
        return outputFiles
            .filter { fileObject -> fileObject.kind == JavaFileObject.Kind.CLASS }
            .map { fileObject ->
                val filePath: Path = Path.of(fileObject.toUri())
                val relativePath: Path = outputDirectory.relativize(filePath)
                relativePath.toString()
                    .replace(FileSystems.getDefault().separator, ".") // Use system-specific separator
                    .replace(".class", "")
            }
    }

    private fun loadPreProcessor(): (String) -> String {
        if (preProcessor == null) {
            return { it } // Identity function if no preprocessor is specified
        }

        log.info("Preprocessor: $preProcessor")
        val preProcessorFile = when {
            Regex(".*\\.java$").matches(preProcessor!!) -> PreProcessor.JavaFile(preProcessor!!)
            Regex(".*\\.kt$").matches(preProcessor!!) -> PreProcessor.KotlinFile(preProcessor!!)
            Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*$").matches(preProcessor!!) -> PreProcessor.ClassName(
                preProcessor!!
            )

            else -> throw MojoExecutionException("Unknown preprocessor: $preProcessor!!")
        }

        try {
            val preProcessorClass: Class<*> = when (preProcessorFile) {
                is PreProcessor.File -> {
                    compileClas(preProcessorFile)
                    val compiledClassNames = loadCompiledClasses()
                    loadClass(compiledClassNames.first())
                }

                is PreProcessor.ClassName -> {
                    loadClass(preProcessorFile.className)
                }
            }

            val preProcessorMethod = preProcessorClass.methods
                .find { m -> m.parameterCount == 1 && m.parameterTypes[0] == String::class.java && m.returnType == String::class.java }
                ?: throw MojoExecutionException("Preprocessor class must have a method that takes a String and returns a String")

            val instance = if (Modifier.isStatic(preProcessorMethod.modifiers)) {
                null
            } else {
                try {
                    preProcessorClass.getDeclaredConstructor().newInstance()
                } catch (e: Exception) {
                    throw MojoExecutionException("Failed to create an instance of preprocessor class.", e)
                }
            }

            return { input: String ->
                try {
                    preProcessorMethod.invoke(instance, input) as String
                } catch (e: Exception) {
                    throw MojoExecutionException("Failed to apply preprocessor", e)
                }
            }
        } catch (e: Exception) {
            throw MojoExecutionException("Failed to load preprocessor class: $preProcessor", e)
        }
    }

    override fun execute() {
        project.addCompileSourceRoot(output)
        val inputPath = getFullPath(input).or(::handleError)

        val sources = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is SourcePath -> {
                val source = inputPath.readFromClasspath<JSON>()
                Source<JSON>(source.name, source.content)
            }

            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> when (inputPath.extension) {
                JSON -> {
                    Source(inputPath.name, inputPath.read())
                }

                Avro -> {
                    Source(inputPath.name, inputPath.read())
                }

                else -> throw JSONFileError()
            }
        }

        ConverterArguments(
            format = format,
            preProcessor = loadPreProcessor(),
            input = nonEmptySetOf(sources),
            output = Directory(getOutPutPath(inputPath, output).or(::handleError)),
            emitters = emitters,
            writer = { filePath, string -> filePath.write(string) },
            error = { throw RuntimeException(it) },
            packageName = PackageName(packageName),
            logger = logger,
            shared = shared,
            strict = strict,
        ).let(::convert)
    }
}
