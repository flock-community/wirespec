package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class JavaFile(path: FilePath) : File(path.copy(extension = FileExtension.Java)) {
    override fun copy(fileName: FileName) = JavaFile(path.copy(fileName = fileName))
}
