package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class JSONFile(path: FilePath) : File(path.copy(extension = FileExtension.JSON)) {
    override fun change(fileName: FileName) = JSONFile(path.copy(fileName = fileName))
    override fun change(directory: DirectoryPath) = JSONFile(path.copy(directory = directory))
}

fun FilePath.toJSONFile() = JSONFile(this)
