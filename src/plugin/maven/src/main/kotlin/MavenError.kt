package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.FileExtension

class IsNotAFileOrDirectory(input: String?) : RuntimeException("Input is not a file or directory: $input.")

class ConvertNeedsAFile : RuntimeException("To convert, please specify a file.")

sealed class SpecificFile(extension: FileExtension) : RuntimeException("No ${extension.name} file found")
class JSONFileError : SpecificFile(FileExtension.JSON)
class WirespecFileError : SpecificFile(FileExtension.Wirespec)

class PickAtLeastOneLanguageOrEmitter : RuntimeException("Pick a least one language or emitter.")
