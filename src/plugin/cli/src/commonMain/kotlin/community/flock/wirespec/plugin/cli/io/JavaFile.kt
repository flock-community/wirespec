package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FullFilePath

class JavaFile(path: FullFilePath) : File(path.copy(extension = FileExtension.Java)) {
    override fun copy(fileName: FileName) = JavaFile(path.copy(fileName = fileName))
}
