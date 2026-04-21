package community.flock.wirespec.plugin.maven.mojo

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.convert
import community.flock.wirespec.plugin.io.ClassPath
import community.flock.wirespec.plugin.io.Directory
import community.flock.wirespec.plugin.io.DirectoryPath
import community.flock.wirespec.plugin.io.FilePath
import community.flock.wirespec.plugin.io.Source
import community.flock.wirespec.plugin.io.Source.Type.JSON
import community.flock.wirespec.plugin.io.getFullPath
import community.flock.wirespec.plugin.io.getOutPutPath
import community.flock.wirespec.plugin.io.or
import community.flock.wirespec.plugin.io.read
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import java.lang.reflect.Modifier

@Suppress("unused")
@Mojo(
    name = "convert",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true,
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

    private fun preProcess(input: String): String {
        if (preProcessor == null) {
            return input // Identity function if no preprocessor is specified
        }

        log.info("Load preprocessor: $preProcessor")

        try {
            val classLoader = getClassLoader(project)
            val preProcessorClass: Class<*> = classLoader.loadClass(preProcessor!!)
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

            return try {
                preProcessorMethod(instance, input) as String
            } catch (e: Exception) {
                throw MojoExecutionException("Failed to apply preprocessor", e)
            }
        } catch (e: Exception) {
            throw MojoExecutionException("Failed to load preprocessor class: $preProcessor", e)
        }
    }

    override fun execute() {
        project.addCompileSourceRoot(output)
        compileSourceDirectory()
        val inputPath = getFullPath(input).or(::handleError)

        val sources = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is ClassPath -> inputPath.readFromClasspath<JSON>().let { (name, content) -> Source<JSON>(name, content) }
            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> when (inputPath.extension) {
                FileExtension.JSON -> Source<JSON>(inputPath.name, inputPath.read())
                FileExtension.Avro -> Source<JSON>(inputPath.name, inputPath.read())
                else -> throw JSONFileError()
            }
                .also { logger.info("Found 1 file to process: $inputPath") }
        }.map(::preProcess)

        val outputDir = Directory(getOutPutPath(inputPath, output).or(::handleError))
        ConverterArguments(
            format = format,
            input = nonEmptySetOf(sources),
            emitters = emitters,
            writer = writer(outputDir),
            error = { throw RuntimeException(it) },
            packageName = PackageName(packageName),
            logger = logger,
            shared = shared,
            strict = strict,
        ).let(::convert)
    }
}
