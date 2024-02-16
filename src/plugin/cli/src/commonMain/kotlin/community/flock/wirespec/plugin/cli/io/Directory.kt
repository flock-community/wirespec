package community.flock.wirespec.plugin.cli.io

expect class Directory(path: String) {

    fun wirespecFiles(): Set<WirespecFile>

}
