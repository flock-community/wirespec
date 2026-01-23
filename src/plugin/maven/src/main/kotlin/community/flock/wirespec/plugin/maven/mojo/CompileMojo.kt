package community.flock.wirespec.plugin.maven.mojo

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.io.ClassPath
import community.flock.wirespec.plugin.io.Directory
import community.flock.wirespec.plugin.io.DirectoryPath
import community.flock.wirespec.plugin.io.FilePath
import community.flock.wirespec.plugin.io.Source
import community.flock.wirespec.plugin.io.Source.Type.Wirespec
import community.flock.wirespec.plugin.io.getFullPath
import community.flock.wirespec.plugin.io.getOutPutPath
import community.flock.wirespec.plugin.io.or
import community.flock.wirespec.plugin.io.read
import community.flock.wirespec.plugin.io.wirespecSources
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
            is ClassPath -> nonEmptySetOf(inputPath.readFromClasspath())
            is DirectoryPath -> Directory(inputPath).wirespecSources(logger).or(::handleError)
            is FilePath -> when (inputPath.extension) {
                FileExtension.Wirespec -> nonEmptySetOf<Source<Wirespec>>(Source(inputPath.name, inputPath.read()))
                else -> throw WirespecFileError()
            }
                .also { logger.info("Found 1 wirespec file to process: $inputPath") }
        }

        val outputDir = Directory(getOutPutPath(inputPath, output).or(::handleError))
        CompilerArguments(
            input = sources,
            emitters = emitters,
            writer = writer(outputDir),
            error = { throw RuntimeException(it) },
            packageName = PackageName(packageName),
            logger = logger,
            shared = shared,
            strict = strict,
        ).let(::compile)
    }
}
