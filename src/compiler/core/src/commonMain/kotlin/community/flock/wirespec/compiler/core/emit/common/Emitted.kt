package community.flock.wirespec.compiler.core.emit.common

data class Emitted(
    val typeName: String,
    val result: String,
)

// Emitter converts the ast to a list of emitted

// Emitted is content to output and what file(+ ext) to write to
// Writer actually outputs this to a given directory

// If there are several emitters, then we should output to (?)
