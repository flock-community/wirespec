package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class ScalaFile(path: FilePath) : File(path.copy(extension = FileExtension.Scala)) {
    override fun changeName(fileName: FileName) = ScalaFile(path.copy(fileName = fileName))
}

fun FilePath.toScalaFile() = ScalaFile(this)
