package community.flock.wirespec.plugin.maven.compiler

import community.flock.wirespec.plugin.maven.community.flock.wirespec.plugin.maven.mojo.PreProcessor
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.File
import java.io.PrintStream

class KotlinCompiler(val project: MavenProject, val log: Log, val outputDir: File) {

    val mavenLogStream: PrintStream = object : PrintStream(System.out) {
        override fun println(s: String?) {
            log.info("[Kotlin Compiler] $s")
        }

        override fun print(s: String?) {
            log.info("[Kotlin Compiler] $s")
        }
    }

    fun compile(kotlinFile: PreProcessor.KotlinFile): Boolean {
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
        return exitCode == ExitCode.OK || exitCode == ExitCode.COMPILATION_ERROR
    }

}