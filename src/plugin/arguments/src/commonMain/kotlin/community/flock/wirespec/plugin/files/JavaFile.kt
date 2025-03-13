package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class JavaFile(path: FilePath) : File(path.copy(extension = FileExtension.Java)) {
    override fun change(fileName: FileName) = JavaFile(path.copy(fileName = fileName))
    override fun change(directory: DirectoryPath) = JavaFile(path.copy(directory = directory))
}

fun FilePath.toJavaFile() = JavaFile(this)
