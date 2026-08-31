package community.flock.wirespec.plugin.cli

import com.github.ajalt.clikt.core.CliktError
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.utils.Logger

internal class NoInputReceived : CliktError("No input file, directory, or stdin received.")

internal class ChooseALogLevel : CliktError("Choose one of these log levels: ${Logger.Level}.")

internal class ConvertNeedsAFile : CliktError("To convert, please specify a file.")

internal class NoClasspathPossible : CliktError("No classpath input possible in cli.")

internal sealed class SpecificFile(extension: FileExtension) : CliktError("No ${extension.name} file found")
internal class JSONFileError : SpecificFile(FileExtension.JSON)
internal class WirespecFileError : SpecificFile(FileExtension.Wirespec)
