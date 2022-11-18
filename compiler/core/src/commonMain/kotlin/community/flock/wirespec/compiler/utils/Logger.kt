package community.flock.wirespec.compiler.utils

abstract class Logger(private val enableLogging: Boolean) {

    open fun warn(s: String) = println(s)

    open fun log(s: String) = if (enableLogging) println(s) else Unit

    fun log(s: String, block: () -> String) = run {
        log(s)
        block()
    }

}

val noLogger = object : Logger(false) {}
