package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.FullFilePath

class WirespecFile(path: FullFilePath) : File(path.copy(extension = Extension.Wirespec)) {
    override fun copy(fileName: String) = WirespecFile(path.copy(fileName = fileName))
}
