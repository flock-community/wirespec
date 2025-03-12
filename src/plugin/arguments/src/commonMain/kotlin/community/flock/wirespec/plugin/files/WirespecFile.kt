package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class WirespecFile(path: FilePath) : File(path.copy(extension = FileExtension.Wirespec)) {
    override fun changeName(fileName: FileName) = WirespecFile(path.copy(fileName = fileName))
}

fun FilePath.toWirespecFile() = WirespecFile(this)
