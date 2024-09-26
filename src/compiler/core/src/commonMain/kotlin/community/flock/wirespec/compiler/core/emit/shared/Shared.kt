package community.flock.wirespec.compiler.core.emit.shared

sealed interface Shared {
    val packageString: String
    val source: String
}
