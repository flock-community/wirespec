package community.flock.wirespec.compiler.cli.io

class JavaFile(path: DirPath) : File(path.copy(extension = Extension.Java))
