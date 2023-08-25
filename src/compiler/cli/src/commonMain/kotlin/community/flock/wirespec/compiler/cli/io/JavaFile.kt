package community.flock.wirespec.compiler.cli.io

class JavaFile(path: FullFilePath) : File(path.copy(extension = Extension.Java)) {
    override fun copy(fileName: String) = JavaFile(path.copy(fileName = fileName))
}
