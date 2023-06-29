package community.flock.wirespec.compiler.cli.io

class JsonFile(path: FullFilePath) : File(path.copy(extension = Extension.Json)) {
    override fun copy(fileName: String) = JsonFile(path.copy(fileName = fileName))
}
