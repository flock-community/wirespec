package community.flock.wirespec.compiler.cli.io

class ScalaFile(path: DirPath) : File(path.copy(extension = Extension.Scala))
