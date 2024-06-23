package community.flock.wirespec.compiler.utils

open class Logger(private val enableLogging: Boolean = false) {

    open fun debug(s: String) = if (enableLogging) println(s) else Unit

    open fun info(s: String) = if (enableLogging) println(s) else Unit

    open fun warn(s: String) = println(s)

    fun info(s: String, block: () -> String) = run {
        info(s)
        block()
    }

}

val noLogger = object : Logger(false) {}
