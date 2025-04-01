package community.flock.wirespec.plugin.maven

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.common.FileExtension.Wirespec
import community.flock.wirespec.compiler.core.emit.common.PackageName
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
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.ResolutionScope

@Suppress("unused")
@Mojo(
    name = "compile",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
)
class CompileMojo : BaseMojo() {

    override fun execute() {
        project.addCompileSourceRoot(output)
        val inputPath = getFullPath(input).or(::handleError)
        val sources = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is SourcePath -> nonEmptySetOf(inputPath.readFromClasspath())
            is DirectoryPath -> Directory(inputPath).wirespecSources().or(::handleError)
            is FilePath -> when (inputPath.extension) {
                Wirespec -> nonEmptySetOf(Source<Wirespec>(inputPath.name, inputPath.read()))
                else -> throw WirespecFileError()
            }
        }
        CompilerArguments(
            input = sources,
            output = Directory(getOutPutPath(inputPath, output).or(::handleError)),
            emitters = emitters,
            writer = { filePath, string -> filePath.write(string) },
            error = { throw RuntimeException(it) },
            packageName = PackageName(packageName),
            logger = logger,
            shared = shared,
            strict = strict,
        ).let(::compile)
    }
}
