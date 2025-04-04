package community.flock.wirespec.plugin.gradle

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.io.Directory
import community.flock.wirespec.plugin.io.DirectoryPath
import community.flock.wirespec.plugin.io.FilePath
import community.flock.wirespec.plugin.io.Source
import community.flock.wirespec.plugin.io.Source.Type.Wirespec
import community.flock.wirespec.plugin.io.SourcePath
import community.flock.wirespec.plugin.io.getFullPath
import community.flock.wirespec.plugin.io.getOutPutPath
import community.flock.wirespec.plugin.io.or
import community.flock.wirespec.plugin.io.read
import community.flock.wirespec.plugin.io.wirespecSources
import community.flock.wirespec.plugin.io.write
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class CompileWirespecTask : BaseWirespecTask() {

    @get:InputDirectory
    @get:Option(option = "input", description = "input directory")
    abstract val input: DirectoryProperty

    @TaskAction
    fun action() {
        val inputPath = getFullPath(input.get().asFile.absolutePath).or(::handleError)
        val outputPath = output.get().asFile.absolutePath
        val sources = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is SourcePath -> nonEmptySetOf(inputPath.readFromClasspath())
            is DirectoryPath -> Directory(inputPath).wirespecSources().or(::handleError)
            is FilePath -> when (inputPath.extension) {
                FileExtension.Wirespec -> nonEmptySetOf(Source<Wirespec>(inputPath.name, inputPath.read()))
                else -> throw WirespecFileError()
            }
        }
        CompilerArguments(
            input = sources,
            output = Directory(getOutPutPath(inputPath, outputPath).or(::handleError)),
            emitters = emitters(),
            writer = { filePath, string -> filePath.write(string) },
            error = { throw RuntimeException(it) },
            packageName = packageNameValue(),
            logger = wirespecLogger,
            shared = shared.getOrElse(true),
            strict = strict.getOrElse(false),
        ).let(::compile)
    }
}
