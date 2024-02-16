package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FullFilePath

class ScalaFile(path: FullFilePath) : File(path.copy(extension = FileExtension.Scala)) {
    override fun copy(fileName: String) = ScalaFile(path.copy(fileName = fileName))
}
