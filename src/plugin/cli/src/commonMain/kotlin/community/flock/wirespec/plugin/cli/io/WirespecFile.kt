package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FullFilePath

class WirespecFile(path: FullFilePath) : File(path.copy(extension = FileExtension.Wirespec)) {
    override fun copy(fileName: String) = WirespecFile(path.copy(fileName = fileName))
}
