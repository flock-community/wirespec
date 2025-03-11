package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class JSONFile(path: FilePath) : File(path.copy(extension = FileExtension.JSON)) {
    override fun copy(fileName: FileName): File = JSONFile(path.copy(fileName = fileName))
}
