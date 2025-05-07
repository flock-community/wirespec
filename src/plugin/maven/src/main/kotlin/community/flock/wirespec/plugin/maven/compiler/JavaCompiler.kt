package community.flock.wirespec.plugin.maven.compiler

import community.flock.wirespec.plugin.maven.community.flock.wirespec.plugin.maven.mojo.PreProcessor
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
     * Compiles a Java or Kotlin source file using the Java Compiler API.
     * @param javaFile Location of the java file to compile.
     * @return True if compilation was successful, false otherwise
     */
    fun compile(javaFile: PreProcessor.JavaFile): Boolean? {
        val sourceFilePath = File(javaFile.filePath)

        if (!sourceFilePath.exists()) {
            log.info("Source file not found: " + sourceFilePath)
            throw MojoFailureException("Source file " + sourceFilePath + " does not exist.")
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

        val compilationUnits = fileManager.getJavaFileObjects(sourceFilePath)
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
