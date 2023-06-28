package community.flock.wirespec.compiler.cli.io

class OpenapiFile(path: FullFilePath) : File(path.copy(extension = Extension.Json)) {
    override fun copy(fileName: String) = OpenapiFile(path.copy(fileName = fileName))
}
