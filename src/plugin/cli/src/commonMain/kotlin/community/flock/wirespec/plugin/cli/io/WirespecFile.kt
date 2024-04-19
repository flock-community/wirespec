package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FullFilePath

class WirespecFile(path: FullFilePath) : File(path.copy(extension = FileExtension.Wirespec)) {
    override fun copy(fileName: FileName): File = WirespecFile(path.copy(fileName = fileName))
}
