package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FullFilePath

class TypeScriptFile(path: FullFilePath) : File(path.copy(extension = FileExtension.TypeScript)) {
    override fun copy(fileName: FileName): File = TypeScriptFile(path.copy(fileName = fileName))
}
