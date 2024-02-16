package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FullFilePath

class KotlinFile(path: FullFilePath) : File(path.copy(extension = FileExtension.Kotlin)) {
    override fun copy(fileName: String) = KotlinFile(path.copy(fileName = fileName))
}
