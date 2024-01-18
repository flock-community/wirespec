package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.FullFilePath

class ScalaFile(path: FullFilePath) : File(path.copy(extension = Extension.Scala)) {
    override fun copy(fileName: String) = ScalaFile(path.copy(fileName = fileName))
}
