package community.flock.wirespec.plugin.io

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.utils.Logger

public sealed class IOError(public val message: String)

internal class CannotAccessFileOrDirectory(input: String) : IOError("Cannot access file or directory: $input.")

internal class IsNotAFileOrDirectory(input: String?) : IOError("Input is not a file or directory: $input.")

private class ChooseALogLevel : IOError("Choose one of these log levels: ${Logger.Level}.")

private class ConvertNeedsAFile : IOError("To convert, please specify a file.")

public sealed class SpecificFile(extension: FileExtension) : IOError("No ${extension.name} file found")
private class JSONFileError : SpecificFile(FileExtension.JSON)
public class WirespecFileError : SpecificFile(FileExtension.Wirespec)

internal class OutputShouldBeADirectory : IOError("Output should be a directory.")

private class ThisShouldNeverHappen : IOError("This should never happen.")

private class PickAtLeastOneLanguageOrEmitter : IOError("Pick at least one language or emitter.")
