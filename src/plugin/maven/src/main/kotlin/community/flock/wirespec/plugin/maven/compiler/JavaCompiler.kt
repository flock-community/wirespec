package community.flock.wirespec.plugin.maven.compiler

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider

class JavaCompiler(val project: MavenProject, val log: Log, val outputDir: File) {

    /**
     * Compiles Java source files located in the specified source directory.
     *
     * This method uses the system Java compiler to compile the source files.
     * It validates the existence of the source directory and ensures the
     * standard Java compiler is available. Compilation options are customized
     * based on the project's classpath and output directory configuration.
     *
     * @param sourceDirectory the directory containing Java source files to be compiled.
     * @return `true` if the compilation succeeds, `false` if it fails, or `null` if the compilation cannot be initiated.
     * @throws MojoFailureException if the specified source directory does not exist.
     * @throws MojoExecutionException if the system Java compiler cannot be located or other critical issues occur.
     */
    fun compile(sourceDirectory: File): Boolean? {

        if (!sourceDirectory.exists()) {
            log.info("Source file not found: " + sourceDirectory)
            throw MojoFailureException("Source file " + sourceDirectory + " does not exist.")
        }

        val compiler = ToolProvider.getSystemJavaCompiler()
        if (compiler == null) {
            throw MojoExecutionException("Could not get system Java compiler. Ensure you are running Maven with a JDK, not just a JRE.")
        }

        val fileManager = compiler.getStandardFileManager(
            null,
            null,
            StandardCharsets.UTF_8,
        ).apply {
            log.info("Output directory: $outputDir")
            setLocation(StandardLocation.CLASS_OUTPUT, listOf(outputDir))
        }

        // Find all Kotlin files in the source directory
        val javaFiles= if (sourceDirectory.isDirectory) {
            sourceDirectory.walkTopDown()
                .filter { it.isFile && it.extension == "java" }
                .toList()
        } else if (sourceDirectory.isFile && sourceDirectory.extension == "java") {
            listOf(sourceDirectory)
        } else {
            emptyList()
        }

        if (javaFiles.isEmpty()) {
            log.info("No Jav files found in sourceDirectory: ${sourceDirectory.absolutePath}")
            return true
        }

        log.info("Found ${javaFiles.size} Kotlin files to compile")

        val compilationUnits = fileManager.getJavaFileObjects(*javaFiles.toTypedArray())
        val options = buildCompilerOptions()
        val diagnostics = DiagnosticCollector<JavaFileObject?>()
        val task = compiler.getTask(
            null, // Output writer
            fileManager,
            diagnostics,
            options,
            null, // Annotation processor class names
            compilationUnits,
        )

        return task.call()
    }

    private fun buildCompilerOptions(): MutableList<String> {
        val options: MutableList<String> = ArrayList<String>()

        if (project.compileClasspathElements != null && project.compileClasspathElements.isNotEmpty()) {
            val classpath = project.compileClasspathElements.joinToString(File.pathSeparator)
            options.addAll(listOf("-classpath", classpath))
            log.debug("Classpath: $classpath")
        } else {
            log.warn("No compile classpath elements found.")
        }

        options.addAll(listOf("-d", outputDir.absolutePath))

        return options
    }
}
