package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class WirespecFile(path: FilePath) : File(path.copy(extension = FileExtension.Wirespec)) {
    override fun change(fileName: FileName) = WirespecFile(path.copy(fileName = fileName))
    override fun change(directory: DirectoryPath) = WirespecFile(path.copy(directory = directory))
}

fun FilePath.toWirespecFile() = WirespecFile(this)
