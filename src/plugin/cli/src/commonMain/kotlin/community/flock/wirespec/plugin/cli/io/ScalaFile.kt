package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath

class ScalaFile(path: FilePath) : File(path.copy(extension = FileExtension.Scala)) {
    override fun copy(fileName: FileName): File = ScalaFile(path.copy(fileName = fileName))
}
