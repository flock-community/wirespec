package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.File
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath

class WirespecFile(path: FilePath) : File(path.copy(extension = FileExtension.Wirespec)) {
    override fun copy(fileName: FileName): File = WirespecFile(path.copy(fileName = fileName))
}
