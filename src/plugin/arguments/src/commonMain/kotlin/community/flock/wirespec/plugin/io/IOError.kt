package community.flock.wirespec.plugin.io

import community.flock.wirespec.compiler.core.emit.FileExtension

public sealed class IOError(public val message: String)

internal class CannotAccessFileOrDirectory(input: String) : IOError("Cannot access file or directory: $input.")

internal class IsNotAFileOrDirectory(input: String?) : IOError("Input is not a file or directory: $input.")

public sealed class SpecificFile(extension: FileExtension) : IOError("No ${extension.name} file found")

public class WirespecFileError : SpecificFile(FileExtension.Wirespec)

internal class OutputShouldBeADirectory : IOError("Output should be a directory.")
