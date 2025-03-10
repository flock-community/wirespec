package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.File
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath

class JsonFile(path: FilePath) : File(path.copy(extension = FileExtension.JSON)) {
    override fun copy(fileName: FileName): File = JsonFile(path.copy(fileName = fileName))
}
