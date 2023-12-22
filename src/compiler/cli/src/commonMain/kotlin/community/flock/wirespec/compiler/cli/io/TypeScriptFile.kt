package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.FullFilePath

class TypeScriptFile(path: FullFilePath) : File(path.copy(extension = Extension.TypeScript)) {
    override fun copy(fileName: String) = TypeScriptFile(path.copy(fileName = fileName))
}
