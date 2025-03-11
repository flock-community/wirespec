package community.flock.wirespec.plugin.files

import community.flock.wirespec.plugin.FileExtension

class KotlinFile(path: FilePath) : File(path.copy(extension = FileExtension.Kotlin)) {
    override fun copy(fileName: FileName): File = KotlinFile(path.copy(fileName = fileName))
}
