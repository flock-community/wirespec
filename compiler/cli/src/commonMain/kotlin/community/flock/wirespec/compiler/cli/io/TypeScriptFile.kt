package community.flock.wirespec.compiler.cli.io

class TypeScriptFile(path: DirPath) : File(path.copy(extension = Extension.TypeScript)) {
    override fun copy(fileName: String) = TypeScriptFile(path.copy(fileName = fileName))
}
