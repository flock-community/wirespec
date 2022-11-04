package community.flock.wirespec.compiler.cli.io

class WireSpecFile(path: DirPath) : File(path.copy(extension = Extension.WireSpec)) {
    override fun copy(fileName: String) = WireSpecFile(path.copy(fileName = fileName))
}
