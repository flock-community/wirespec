package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class ScalaFile(path: FilePath) : File(path.copy(extension = FileExtension.Scala)) {
    override fun change(fileName: FileName) = ScalaFile(path.copy(fileName = fileName))
    override fun change(directory: DirectoryPath) = ScalaFile(path.copy(directory = directory))
}

fun FilePath.toScalaFile() = ScalaFile(this)
