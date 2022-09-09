package community.flock.wirespec.compiler.cli.io

class KotlinFile(path: Path) : File(path.copy(extension = Extension.Kotlin)) {
    companion object {
        val extension = Extension.Kotlin
    }
}
