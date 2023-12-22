package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.FullFilePath

class JsonFile(path: FullFilePath) : File(path.copy(extension = Extension.Json)) {
    override fun copy(fileName: String) = JsonFile(path.copy(fileName = fileName))
}
