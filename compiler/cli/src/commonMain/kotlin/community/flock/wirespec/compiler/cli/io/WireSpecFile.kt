package community.flock.wirespec.compiler.cli.io

class WireSpecFile(path: Path) : File(path.copy(extension = Extension.WireSpec))
