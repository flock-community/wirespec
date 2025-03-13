package community.flock.wirespec.plugin.maven

import arrow.core.Either.Companion.catch
import arrow.core.getOrElse
import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.WirespecFile
import community.flock.wirespec.plugin.files.plus
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = "compile", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
open class CompileMojo : BaseMojo() {

    @Parameter(required = true)
    protected lateinit var languages: List<Language>

    @Parameter
    protected var shared: Boolean = true

    override fun execute() {
        project.addCompileSourceRoot(output)
        val input = catch {
            when (val it = getFullPath(input)) {
                null -> throw IsNotAFileOrDirectory(null)
                is DirectoryPath -> Directory(it).wirespecFiles()
                is FilePath -> when (it.extension) {
                    FileExtension.Wirespec -> nonEmptySetOf(WirespecFile(it))
                    else -> throw WirespecFileError()
                }
            }
        }.getOrElse {
            println("input: $it")
            throw it
        }
        val output = catch {
            when (val it = getFullPath(output, true)) {
                null -> Directory(input.first().path + "/out")
                is DirectoryPath -> Directory(it)
                is FilePath -> throw OutputShouldBeADirectory()
            }
        }.getOrElse {
            println("output: $it")
            throw it
        }
        CompilerArguments(
            inputFiles = input,
            outputDirectory = output,
            reader = {
                catch { it.read() }.getOrElse {
                    println(it)
                    throw it
                }
            },
            writer = { file, string -> file.write(string) },
            error = { throw RuntimeException(it) },
            languages = languages.toNonEmptySetOrNull() ?: throw ThisShouldNeverHappen(),
            packageName = PackageName(packageName),
            logger = logger,
            shared = shared,
            strict = strict,
        ).let(::compile)
    }
}
