package community.flock.wirespec.compiler.core.emit

data class Emitted @JvmOverloads constructor(
    val file: String,
    val result: String,
    val test: Boolean = false,
)
