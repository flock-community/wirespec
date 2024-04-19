package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FullFilePath

class JsonFile(path: FullFilePath) : File(path.copy(extension = FileExtension.Json)) {
    override fun copy(fileName: FileName): File = JsonFile(path.copy(fileName = fileName))
}
