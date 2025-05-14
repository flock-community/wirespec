package community.flock.wirespec.plugin.cli

import com.github.ajalt.clikt.core.CliktError
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.utils.Logger

class IsNotAFileOrDirectory(input: String?) : CliktError("Input is not a file or directory: $input.")

class NoInputReceived : CliktError("No input file, directory, or stdin received.")

class ChooseALogLevel : CliktError("Choose one of these log levels: ${Logger.Level}.")

class ConvertNeedsAFile : CliktError("To convert, please specify a file.")

class NoClasspathPossible : CliktError("No classpath input possible in cli.")

sealed class SpecificFile(extension: FileExtension) : CliktError("No ${extension.name} file found")
class JSONFileError : SpecificFile(FileExtension.JSON)
class WirespecFileError : SpecificFile(FileExtension.Wirespec)
