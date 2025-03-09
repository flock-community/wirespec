package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.File
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath

class KotlinFile(path: FilePath) : File(path.copy(extension = FileExtension.Kotlin)) {
    override fun copy(fileName: FileName): File = KotlinFile(path.copy(fileName = fileName))
}
