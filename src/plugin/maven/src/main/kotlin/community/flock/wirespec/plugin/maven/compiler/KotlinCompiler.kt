package community.flock.wirespec.plugin.maven.compiler

import community.flock.wirespec.plugin.maven.community.flock.wirespec.plugin.maven.mojo.PreProcessor
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Path
import javax.tools.JavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider

class KotlinCompiler(val project: MavenProject, val log: Log, val outputDir: File)  {

    val mavenLogStream: PrintStream = object : PrintStream(System.out) {
        override fun println(s: String?) { log.info("[Kotlin Compiler] $s") }
        override fun print(s: String?) { log.info("[Kotlin Compiler] $s") }
    }

     fun compile(kotlinFile: PreProcessor.KotlinFile): List<String> {
        log.info("Compiling Kotlin file: ${kotlinFile.filePath}")

        val kotlinSourceFile = File(kotlinFile.filePath)
        val compiler = K2JVMCompiler()

        val args = arrayOf(
            kotlinSourceFile.absolutePath,
            "-d", outputDir.absolutePath,
            "-cp", project.compileClasspathElements.joinToString(File.pathSeparator),
            "-no-stdlib",
            "-version",
        )

        val exitCode: ExitCode = compiler.exec(mavenLogStream, *args)

         val success = exitCode == ExitCode.OK || exitCode == ExitCode.COMPILATION_ERROR
         if (success) {
             val compiler = ToolProvider.getSystemJavaCompiler()
             if (compiler == null) {
                 throw MojoExecutionException("Could not get system Java compiler. Ensure you are running Maven with a JDK, not just a JRE.")
             }

             val fileManager = compiler.getStandardFileManager(
                 null,
                 null,
                 StandardCharsets.UTF_8
             ).apply {
                 log.info("Output directory: $outputDir")
                 setLocation(StandardLocation.CLASS_OUTPUT, listOf(outputDir))
             }

             try {
                 val outputLocation = StandardLocation.CLASS_OUTPUT
                 val outputFiles = fileManager.list(outputLocation, "", mutableSetOf(JavaFileObject.Kind.CLASS), true)
                 log.info("outputFiles: $outputFiles")

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

}