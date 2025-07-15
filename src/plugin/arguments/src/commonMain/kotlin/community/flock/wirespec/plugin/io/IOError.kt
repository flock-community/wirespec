package community.flock.wirespec.plugin.io

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.utils.Logger

sealed class IOError(val message: String)

class CannotAccessFileOrDirectory(input: String) : IOError("Cannot access file or directory: $input.")

class IsNotAFileOrDirectory(input: String?) : IOError("Input is not a file or directory: $input.")

class ChooseALogLevel : IOError("Choose one of these log levels: ${Logger.Level}.")

class ConvertNeedsAFile : IOError("To convert, please specify a file.")

sealed class SpecificFile(extension: FileExtension) : IOError("No ${extension.name} file found")
class JSONFileError : SpecificFile(FileExtension.JSON)
class WirespecFileError : SpecificFile(FileExtension.Wirespec)

class OutputShouldBeADirectory : IOError("Output should be a directory.")

class ThisShouldNeverHappen : IOError("This should never happen.")

class PickAtLeastOneLanguageOrEmitter : IOError("Pick a least one language or emitter.")
