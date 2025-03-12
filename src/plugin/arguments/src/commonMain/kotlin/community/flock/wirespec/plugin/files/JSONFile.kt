package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class JSONFile(path: FilePath) : File(path.copy(extension = FileExtension.JSON)) {
    override fun changeName(fileName: FileName) = JSONFile(path.copy(fileName = fileName))
}

fun FilePath.toJSONFile() = JSONFile(this)
