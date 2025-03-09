package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.File
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath

class JavaFile(path: FilePath) : File(path.copy(extension = FileExtension.Java)) {
    override fun copy(fileName: FileName) = JavaFile(path.copy(fileName = fileName))
}
