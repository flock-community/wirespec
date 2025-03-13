package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.FileExtension

class CannotAccessFileOrDirectory(input: String) : RuntimeException("Cannot access file or directory: $input.")

class IsNotAFileOrDirectory(input: String?) : RuntimeException("Input is not a file or directory: $input.")

class ChooseALogLevel : RuntimeException("Choose one of these log levels: ${Logger.Level}.")

class ConvertNeedsAFile : RuntimeException("To convert, please specify a file.")

sealed class SpecificFile(extension: FileExtension) : RuntimeException("No ${extension.name} file found")
class JSONFileError : SpecificFile(FileExtension.JSON)
class WirespecFileError : SpecificFile(FileExtension.Wirespec)

class OutputShouldBeADirectory : RuntimeException("Output should be a directory.")

class ThisShouldNeverHappen : RuntimeException("This should never happen.")
