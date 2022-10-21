package community.flock.wirespec.compiler.cli.io

class WireSpecFile(path: DirPath) : File(path.copy(extension = Extension.WireSpec))
