package community.flock.wirespec.compiler.utils

abstract class Logger(private val enableLogging: Boolean) {

    fun warn(s: String) = println(s)

    fun log(s: String) = if (enableLogging) println(s) else Unit

    fun log(s: String, block: () -> String) = run {
        log(s)
        block()
    }

}
