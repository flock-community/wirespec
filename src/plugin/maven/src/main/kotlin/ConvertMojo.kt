package community.flock.wirespec.plugin.maven

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
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope


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
    private lateinit var preProcessor: String

    /**
     * Loads the preprocessor class and returns a function that applies the preprocessor to a string.
     * If no preprocessor is specified, returns the identity function.
     */
    private fun loadPreProcessor(): (String) -> String {
        if (preProcessor == null) {
            return { it } // Identity function if no preprocessor is specified
        }

        try {
            var preprocessorClass: Class<*>? = null

            log.info("Preprocessor class not found: $preProcessor. Attempting to compile it...")

            val compiled = compileJavaClass(preProcessor)
            if (compiled) {
                val classLoader = getClassLoader(project)
                try {
                    preprocessorClass = classLoader.loadClass(preProcessor)
                } catch (e2: ClassNotFoundException) {
                    log.error("Still could not load preprocessor class after compilation: ${e2.message}")
                    throw RuntimeException("Failed to load preprocessor class $preProcessor after compilation", e2)
                }
            } else {
                throw RuntimeException("Failed to compile preprocessor class $preProcessor")
            }


            if (preprocessorClass == null) {
                throw RuntimeException("Failed to load preprocessor class $preProcessor")
            }

            // Try to find a method that takes a String and returns a String
            val method = preprocessorClass.methods.find { method ->
                method.parameterCount == 1 &&
                        method.parameterTypes[0] == String::class.java &&
                        method.returnType == String::class.java
            }
                ?: throw RuntimeException("Preprocessor class $preProcessor must have a method that takes a String and returns a String")

            // Create an instance of the preprocessor class if the method is not static
            val instance = if (java.lang.reflect.Modifier.isStatic(method.modifiers)) {
                null
            } else {
                try {
                    preprocessorClass.getDeclaredConstructor().newInstance()
                } catch (e: Exception) {
                    throw RuntimeException(
                        "Failed to create an instance of preprocessor class $preProcessor. Make sure it has a public no-arg constructor.",
                        e
                    )
                }
            }

            // Return a function that applies the preprocessor
            return { input: String ->
                try {
                    method.invoke(instance, input) as String
                } catch (e: Exception) {
                    throw RuntimeException("Failed to apply preprocessor $preProcessor to input", e)
                }
            }
        } catch (e: Exception) {
            log.error("Failed to load preprocessor class: ${e.message}")
            throw RuntimeException("Failed to load preprocessor class $preProcessor", e)
        }
    }

    override fun execute() {
        project.addCompileSourceRoot(output)
        val inputPath = getFullPath(input).or(::handleError)

        // Load the preprocessor
        // If a preprocessor is specified but can't be loaded, this will throw an exception
        // with a helpful error message
        val preprocess = try {
            loadPreProcessor()
        } catch (e: Exception) {
            log.error("Failed to load or compile the preprocessor: ${e.message}")
            log.error("If you're using a Kotlin preprocessor, make sure it's compiled before the convert goal runs.")
            log.error("You can set the phase of the convert goal to 'process-classes' in your pom.xml.")
            throw e
        }

        val sources = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is SourcePath -> {
                val source = inputPath.readFromClasspath<community.flock.wirespec.plugin.io.Source.Type.JSON>()
                Source<JSON>(source.name, preprocess(source.content))
            }

            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> when (inputPath.extension) {
                JSON -> {
                    Source<JSON>(inputPath.name, preprocess(inputPath.read()))
                }

                Avro -> {
                    Source<JSON>(inputPath.name, preprocess(inputPath.read()))
                }

                else -> throw JSONFileError()
            }
        }

        ConverterArguments(
            format = format,
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
