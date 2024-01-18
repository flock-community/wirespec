package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.FullFilePath

class JavaFile(path: FullFilePath) : File(path.copy(extension = Extension.Java)) {
    override fun copy(fileName: String) = JavaFile(path.copy(fileName = fileName))
}
