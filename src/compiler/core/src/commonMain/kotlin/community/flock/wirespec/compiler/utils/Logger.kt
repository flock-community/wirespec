package community.flock.wirespec.compiler.utils

import community.flock.wirespec.compiler.utils.Logger.Level.DEBUG
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.compiler.utils.Logger.Level.INFO
import community.flock.wirespec.compiler.utils.Logger.Level.WARN

open class Logger(private val logLevel: Level? = ERROR) {

    open fun debug(s: String) = when (logLevel) {
        null -> Unit
        DEBUG, INFO, WARN, ERROR -> println(s)
    }

    open fun info(s: String) = when (logLevel) {
        null, DEBUG -> Unit
        INFO, WARN, ERROR -> println(s)
    }

    open fun warn(s: String) = when (logLevel) {
        null, DEBUG, INFO -> Unit
        WARN, ERROR -> println(s)
    }

    open fun error(s: String) = when (logLevel) {
        null, DEBUG, INFO, WARN -> Unit
        ERROR -> println(s)
    }

    enum class Level {
        DEBUG, INFO, WARN, ERROR;

        companion object {
            override fun toString() = entries.joinToString(", ")
        }
    }

}

val noLogger = object : Logger(logLevel = null) {}
