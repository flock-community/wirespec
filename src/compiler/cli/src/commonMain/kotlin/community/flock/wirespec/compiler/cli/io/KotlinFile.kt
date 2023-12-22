package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.FullFilePath

class KotlinFile(path: FullFilePath) : File(path.copy(extension = Extension.Kotlin)) {
    override fun copy(fileName: String) = KotlinFile(path.copy(fileName = fileName))
}
