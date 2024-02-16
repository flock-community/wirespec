package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FullFilePath

class JsonFile(path: FullFilePath) : File(path.copy(extension = FileExtension.Json)) {
    override fun copy(fileName: String) = JsonFile(path.copy(fileName = fileName))
}
