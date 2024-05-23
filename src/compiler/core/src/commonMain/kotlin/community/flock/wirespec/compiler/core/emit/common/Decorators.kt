package community.flock.wirespec.compiler.core.emit.common

data class Decorators(
    val declaration:String? = null,
    val endpoint:String? = null,
)

fun String?.emitDecorator() = this?.let { "$it\n" }.orEmpty()
