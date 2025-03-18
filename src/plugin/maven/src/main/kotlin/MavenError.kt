package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.utils.Logger

class CannotAccessFileOrDirectory(input: String) : RuntimeException("Cannot access file or directory: $input.")

class IsNotAFileOrDirectory(input: String?) : RuntimeException("Input is not a file or directory: $input.")

class ChooseALogLevel : RuntimeException("Choose one of these log levels: ${Logger.Level}.")

class ConvertNeedsAFile : RuntimeException("To convert, please specify a file.")

sealed class SpecificFile(extension: FileExtension) : RuntimeException("No ${extension.name} file found")
class JSONFileError : SpecificFile(FileExtension.JSON)
class WirespecFileError : SpecificFile(FileExtension.Wirespec)

class OutputShouldBeADirectory : RuntimeException("Output should be a directory.")

class ThisShouldNeverHappen : RuntimeException("This should never happen.")

class PickAtLeastOneLanguageOrEmitter : RuntimeException("Pick a least one language or emitter.")
