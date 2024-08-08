package community.flock.wirespec.compiler.utils

import community.flock.wirespec.compiler.utils.Logger.Level.DEBUG
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.compiler.utils.Logger.Level.INFO
import community.flock.wirespec.compiler.utils.Logger.Level.WARN

open class Logger(logLevel: Level? = ERROR) {

    open val isDebug = when (logLevel) {
        null -> false
        DEBUG, INFO, WARN, ERROR -> true
    }

    open val isInfo = when (logLevel) {
        null, DEBUG -> false
        INFO, WARN, ERROR -> true
    }

    open val isWarn = when (logLevel) {
        null, DEBUG, INFO -> false
        WARN, ERROR -> true
    }

    open val isError = when (logLevel) {
        null, DEBUG, INFO, WARN -> false
        ERROR -> true
    }

    open fun debug(string: String) = string logIf isDebug
    open fun info(string: String) = string logIf isInfo
    open fun warn(string: String) = string logIf isWarn
    open fun error(string: String) = string logIf isError

    private infix fun String.logIf(b: Boolean) = if (b) println(this) else Unit

    enum class Level {
        DEBUG, INFO, WARN, ERROR;

        companion object {
            override fun toString() = entries.joinToString(", ")
        }
    }

}

val noLogger = object : Logger(logLevel = null) {}
