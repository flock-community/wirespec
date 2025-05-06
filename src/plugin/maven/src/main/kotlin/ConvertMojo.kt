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
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.File
import java.io.PrintStream
import java.util.*


sealed interface PreProcessor {
    data class KotlinFile(val filePath: String) : PreProcessor
    data class JavaFile(val filePath: String) : PreProcessor
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


    private fun internalizePreProcessor(input: String): PreProcessor = when {
        Regex(".*\\.java$").matches(input) ->
            PreProcessor.JavaFile(input)

        Regex(".*\\.kt$").matches(input) ->
            PreProcessor.KotlinFile(input)

        Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*$").matches(input) ->
            PreProcessor.ClassName(input)

        else -> error("Unknown preprocessor: $input")
    }

    private fun loadKotlinClass(kotlinFile: PreProcessor.KotlinFile): Class<*> {
        val compiledClassNames = compileKotlinClass(kotlinFile)
        log.info("Compiled preprocessor class: $compiledClassNames")
        val classLoader = getClassLoader(project)
        return classLoader.loadClass("community.flock.wirespec.example.maven.preprocessor.ExamplePreProcessor")
    }

    private fun compileKotlinClass(kotlinFile: PreProcessor.KotlinFile): Boolean {

        val kotlinSourceFile = File(kotlinFile.filePath)
        val outputDir = classOutputDir()

        val compiler = K2JVMCompiler()

        log.info("Compiling Kotlin file: ${kotlinSourceFile.absolutePath}")

        val args = arrayOf(
            kotlinSourceFile.absolutePath,
            "-d", outputDir.absolutePath,
            "-cp", project.compileClasspathElements.joinToString(File.pathSeparator),
            "-no-stdlib",
            "-no-reflect",
            "-version",
        )


        val mavenLogStream: PrintStream = object : PrintStream(System.out) {
            override fun println(x: String?) {
                log.info("[Kotlin Compiler] " + x)
            }

            override fun print(s: String?) {
                log.info("[Kotlin Compiler] " + s)
            }
        }

        val exitCode: ExitCode = compiler.exec(mavenLogStream, *args)

        log.info("Exit code: $exitCode")

        return exitCode == ExitCode.OK || exitCode == ExitCode.COMPILATION_ERROR // Consider COMPILATION_ERROR as success for the *execution* but log errors


    }

    private fun loadJavaClass(javaFile: PreProcessor.JavaFile): Class<*> {
        val compiledClassNames = compileJavaClass(javaFile)
        log.info("Compiled preprocessor class: $compiledClassNames")

        if (compiledClassNames.isNotEmpty()) {
            val classLoader = getClassLoader(project)
            return classLoader.loadClass(compiledClassNames.first())
        } else {
            throw RuntimeException("Failed to compile preprocessor class $preProcessor")
        }
    }

    /**
     * Loads the preprocessor class and returns a function that applies the preprocessor to a string.
     * If no preprocessor is specified, returns the identity function.
     */
    private fun loadPreProcessor(): (String) -> String {
        if (preProcessor == null) {
            return { it } // Identity function if no preprocessor is specified
        }

        try {
            log.info("Compile pre-processor: $preProcessor")
            val preProcessorFile = internalizePreProcessor(preProcessor!!)
            val preprocessorClass: Class<*>? = when (preProcessorFile) {
                is PreProcessor.JavaFile -> loadJavaClass(preProcessorFile)
                is PreProcessor.KotlinFile -> loadKotlinClass(preProcessorFile)
                is PreProcessor.ClassName -> TODO()
            }

            if (preprocessorClass == null) {
                throw RuntimeException("Failed to load preprocessor class $preProcessorFile")
            }


            // Try to find a method that takes a String and returns a String
            val method = preprocessorClass.methods.find { method ->
                log.info(method.name.toString())
                log.info(method.parameterCount.toString())
                log.info(method.returnType.toString())
                method.parameterCount == 1 &&
                        method.parameterTypes[0] == String::class.java &&
                        method.returnType == String::class.java
            }
                ?: throw RuntimeException("Preprocessor class $preProcessorFile must have a method that takes a String and returns a String")

            // Create an instance of the preprocessor class if the method is not static
            val instance = if (java.lang.reflect.Modifier.isStatic(method.modifiers)) {
                null
            } else {
                try {
                    preprocessorClass.getDeclaredConstructor().newInstance()
                } catch (e: Exception) {
                    throw MojoExecutionException(
                        "Failed to create an instance of preprocessor class $preProcessorFile. Make sure it has a public no-arg constructor.",
                        e
                    )
                }
            }

            return { input: String ->
                try {
                    method.invoke(instance, input) as String
                } catch (e: Exception) {
                    throw MojoExecutionException("Failed to apply preprocessor $preProcessorFile to input", e)
                }
            }
        } catch (e: Exception) {
            log.error("Failed to load preprocessor class: ${e.message}")
            throw MojoExecutionException("Failed to load preprocessor class: $preProcessor", e)
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
                JSON -> { Source(inputPath.name, preprocess(inputPath.read())) }
                Avro -> { Source(inputPath.name, preprocess(inputPath.read())) }
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

object Services {
    val EMPTY = org.jetbrains.kotlin.config.Services.EMPTY
}

