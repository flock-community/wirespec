package community.flock.wirespec.compiler.utils

import community.flock.wirespec.compiler.utils.Logger.Level.DEBUG
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.compiler.utils.Logger.Level.INFO
import community.flock.wirespec.compiler.utils.Logger.Level.WARN

open class Logger(logLevel: Level?) {

    open val shouldDebugLog = when (logLevel) {
        DEBUG -> true
        null, INFO, WARN, ERROR -> false
    }

    open val shouldInfoLog = when (logLevel) {
        DEBUG, INFO -> true
        null, WARN, ERROR -> false
    }

    open val shouldWarnLog = when (logLevel) {
        DEBUG, INFO, WARN -> true
        null, ERROR -> false
    }

    open val shouldErrorLog = when (logLevel) {
        DEBUG, INFO, WARN, ERROR -> true
        null -> false
    }

    open fun debug(string: String) = string logIf shouldDebugLog
    open fun info(string: String) = string logIf shouldInfoLog
    open fun warn(string: String) = string logIf shouldWarnLog
    open fun error(string: String) = string logIf shouldErrorLog

    private infix fun String.logIf(b: Boolean) = if (b) println(this) else Unit

    enum class Level {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        ;

        companion object {
            override fun toString() = entries.joinToString(", ")
        }
    }
}

interface HasLogger {
    val logger: Logger
}

interface NoLogger : HasLogger {
    override val logger get() = noLogger
}

val noLogger = object : Logger(logLevel = null) {}
