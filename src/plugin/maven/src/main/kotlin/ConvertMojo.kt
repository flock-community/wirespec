package community.flock.wirespec.plugin.maven

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.common.FileExtension.Avro
import community.flock.wirespec.compiler.core.emit.common.FileExtension.JSON
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.convert
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.Source
import community.flock.wirespec.plugin.files.Source.Type.JSON
import community.flock.wirespec.plugin.files.SourcePath
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope

@Mojo(
    name = "convert",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
)
class ConvertMojo : BaseMojo() {

    @Parameter(required = true)
    private lateinit var format: Format

    override fun execute() {
        project.addCompileSourceRoot(output)
        val inputPath = getFullPath(input)
        val sources = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is SourcePath -> inputPath.readFromClasspath()
            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> when (inputPath.extension) {
                JSON -> Source<JSON>(inputPath.name, inputPath.read())
                Avro -> Source<JSON>(inputPath.name, inputPath.read())
                else -> throw JSONFileError()
            }
        }
        ConverterArguments(
            format = format,
            input = nonEmptySetOf(sources),
            output = Directory(getOutPutPath(inputPath)),
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
