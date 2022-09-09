package community.flock.wirespec.compiler.cli.io

expect class Directory(path: String) {

    fun wireSpecFiles(): Set<WireSpecFile>

}
