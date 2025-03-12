package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class TypeScriptFile(path: FilePath) : File(path.copy(extension = FileExtension.TypeScript)) {
    override fun changeName(fileName: FileName): File = TypeScriptFile(path.copy(fileName = fileName))
}

fun FilePath.toTypeScriptFile() = TypeScriptFile(this)
