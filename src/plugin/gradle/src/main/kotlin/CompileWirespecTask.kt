package community.flock.wirespec.plugin.gradle

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.common.FileExtension.Wirespec
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.Source
import community.flock.wirespec.plugin.files.Source.Type.Wirespec
import community.flock.wirespec.plugin.files.SourcePath
import org.gradle.api.tasks.TaskAction

abstract class CompileWirespecTask : BaseWirespecTask() {

    @TaskAction
    fun action() {
        val inputPath = getFullPath(input.get().asFile.absolutePath)
        val sources = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is SourcePath -> nonEmptySetOf(inputPath.readFromClasspath())
            is DirectoryPath -> Directory(inputPath).wirespecFiles()
            is FilePath -> when (inputPath.extension) {
                Wirespec -> nonEmptySetOf(Source<Wirespec>(inputPath.name, inputPath.read()))
                else -> throw WirespecFileError()
            }
        }
        CompilerArguments(
            input = sources,
            output = Directory(getOutPutPath(inputPath)),
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
