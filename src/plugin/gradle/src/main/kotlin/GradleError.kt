package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.plugin.io.ClassPath

internal class IsNotAFileOrDirectory(input: String?) : RuntimeException("Input is not a file or directory: $input.")

internal class ConvertNeedsAFile : RuntimeException("To convert, please specify a file.")

internal sealed class SpecificFile(extension: FileExtension) : RuntimeException("No ${extension.name} file found")
internal class JSONFileError : SpecificFile(FileExtension.JSON)
internal class WirespecFileError : SpecificFile(FileExtension.Wirespec)

internal class NoInput : RuntimeException("Set the input or the classpathInput.")

internal class InputAndClasspathInput : RuntimeException("Set the input or the classpathInput, not both.")

internal class NotOnClasspath(path: ClassPath) : RuntimeException("Could not find $path on the classpath of the task.")
