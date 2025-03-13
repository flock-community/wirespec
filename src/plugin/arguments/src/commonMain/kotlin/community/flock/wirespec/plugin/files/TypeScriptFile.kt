package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class TypeScriptFile(path: FilePath) : File(path.copy(extension = FileExtension.TypeScript)) {
    override fun change(fileName: FileName): File = TypeScriptFile(path.copy(fileName = fileName))
    override fun change(directory: DirectoryPath) = TypeScriptFile(path.copy(directory = directory))
}

fun FilePath.toTypeScriptFile() = TypeScriptFile(this)
