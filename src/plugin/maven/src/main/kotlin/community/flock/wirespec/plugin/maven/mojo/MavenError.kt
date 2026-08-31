package community.flock.wirespec.plugin.maven.mojo

import community.flock.wirespec.compiler.core.emit.FileExtension

internal class IsNotAFileOrDirectory(input: String?) : RuntimeException("Input is not a file or directory: $input.")

internal class ConvertNeedsAFile : RuntimeException("To convert, please specify a file.")

internal sealed class SpecificFile(extension: FileExtension) : RuntimeException("No ${extension.name} file found")
internal class JSONFileError : SpecificFile(FileExtension.JSON)
internal class WirespecFileError : SpecificFile(FileExtension.Wirespec)

internal class PickAtLeastOneLanguageOrEmitter : RuntimeException("Pick at least one language or emitter.")
