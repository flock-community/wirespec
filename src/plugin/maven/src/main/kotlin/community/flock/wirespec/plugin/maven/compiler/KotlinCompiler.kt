package community.flock.wirespec.plugin.maven.compiler

import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.File
import java.io.PrintStream

class KotlinCompiler(val project: MavenProject, val log: Log, val outputDir: File) {

    val mavenLogStream: PrintStream = object : PrintStream(System.out) {
        override fun println(s: String?) = log.info(s)
        override fun print(s: String?) = log.info(s)
    }

    fun compile(sourceDirectory: File): Boolean {
        log.info("Compiling Kotlin files in sourceDirectory: ${sourceDirectory.absolutePath}")
        val compiler = K2JVMCompiler()

        // Find all Kotlin files in the source directory
        val kotlinFiles = if (sourceDirectory.isDirectory) {
            sourceDirectory.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()
        } else if (sourceDirectory.isFile && sourceDirectory.extension == "kt") {
            listOf(sourceDirectory)
        } else {
            emptyList()
        }

        if (kotlinFiles.isEmpty()) {
            log.info("No Kotlin files found in sourceDirectory: ${sourceDirectory.absolutePath}")
            return true
        }

        log.info("Found ${kotlinFiles.size} Kotlin files to compile")

        val args = mutableListOf<String>().apply {
            addAll(kotlinFiles.map { it.absolutePath })
            add("-d")
            add(outputDir.absolutePath)
            add("-cp")
            add(project.compileClasspathElements.joinToString(File.pathSeparator))
            add("-no-stdlib")
            add("-version")
        }.toTypedArray()

        val exitCode: ExitCode = compiler.exec(mavenLogStream, *args)
        return exitCode == ExitCode.OK || exitCode == ExitCode.COMPILATION_ERROR
    }
}
