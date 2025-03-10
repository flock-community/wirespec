package community.flock.wirespec.plugin.cli

import com.github.ajalt.clikt.core.CliktError
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.FileExtension

class CannotAccessFileOrDirectory(input: String) : CliktError("Cannot access file or directory: $input.")

class IsNotAFileOrDirectory(input: String?) : CliktError("Input is not a file or directory: $input.")

class ChooseALogLevel : CliktError("Choose one of these log levels: ${Logger.Level}.")

class ConvertNeedsAFile : CliktError("To convert, please specify a file.")

sealed class SpecificFile(extension: FileExtension) : CliktError("No ${extension.name} file found")
class JSONFileError : SpecificFile(FileExtension.JSON)
class WirespecFileError : SpecificFile(FileExtension.Wirespec)

class OutputShouldBeADirectory : CliktError("Output should be a directory.")

class ThisShouldNeverHappen : CliktError("This should never happen.")
