package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class KotlinFile(path: FilePath) : File(path.copy(extension = FileExtension.Kotlin)) {
    override fun changeName(fileName: FileName) = KotlinFile(path.copy(fileName = fileName))
}

fun FilePath.toKotlinFile() = KotlinFile(this)
