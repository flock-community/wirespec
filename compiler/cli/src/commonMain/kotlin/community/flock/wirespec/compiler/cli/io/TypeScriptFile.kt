package community.flock.wirespec.compiler.cli.io

class TypeScriptFile(path: DirPath) : File(path.copy(extension = Extension.TypeScript)) {
    companion object {
        val extension = Extension.TypeScript
    }
}
