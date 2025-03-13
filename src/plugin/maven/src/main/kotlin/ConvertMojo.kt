package community.flock.wirespec.plugin.maven

import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.convert
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.JSONFile
import community.flock.wirespec.plugin.files.plus
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = "convert", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class ConvertMojo : CompileMojo() {

    @Parameter(required = true)
    private lateinit var format: Format

    override fun execute() {
        project.addCompileSourceRoot(output)
        val input = when (val it = getFullPath(input)) {
            null -> throw IsNotAFileOrDirectory(null)
            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> when (it.extension) {
                FileExtension.JSON -> JSONFile(it)
                else -> throw JSONFileError()
            }
        }
        val output = when (val it = getFullPath(output, true)) {
            null -> Directory(input.path + "/out")
            is DirectoryPath -> Directory(it)
            is FilePath -> throw OutputShouldBeADirectory()
        }
        ConverterArguments(
            format = format,
            inputFiles = nonEmptySetOf(input),
            outputDirectory = output,
            reader = { it.read() },
            writer = { file, string -> file.write(string) },
            error = { throw RuntimeException(it) },
            languages = languages.toNonEmptySetOrNull() ?: nonEmptySetOf(Wirespec),
            packageName = PackageName(packageName),
            logger = logger,
            shared = shared,
            strict = strict,
        ).let(::convert)
    }
}
