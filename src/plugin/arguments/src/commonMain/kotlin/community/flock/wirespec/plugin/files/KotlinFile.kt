package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class KotlinFile(path: FilePath) : File(path.copy(extension = FileExtension.Kotlin)) {
    override fun change(fileName: FileName) = KotlinFile(path.copy(fileName = fileName))
    override fun change(directory: DirectoryPath) = KotlinFile(path.copy(directory = directory))
}

fun FilePath.toKotlinFile() = KotlinFile(this)
