package community.flock.wirespec.compiler.utils

import community.flock.wirespec.compiler.utils.Logger.Level.DEBUG
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.compiler.utils.Logger.Level.INFO
import community.flock.wirespec.compiler.utils.Logger.Level.WARN

public open class Logger(logLevel: Level?) {

    public open val shouldDebugLog: Boolean = when (logLevel) {
        DEBUG -> true
        null, INFO, WARN, ERROR -> false
    }

    public open val shouldInfoLog: Boolean = when (logLevel) {
        DEBUG, INFO -> true
        null, WARN, ERROR -> false
    }

    public open val shouldWarnLog: Boolean = when (logLevel) {
        DEBUG, INFO, WARN -> true
        null, ERROR -> false
    }

    public open val shouldErrorLog: Boolean = when (logLevel) {
        DEBUG, INFO, WARN, ERROR -> true
        null -> false
    }

    public open fun debug(string: String): Unit = string logIf shouldDebugLog
    public open fun info(string: String): Unit = string logIf shouldInfoLog
    public open fun warn(string: String): Unit = string logIf shouldWarnLog
    public open fun error(string: String): Unit = string logIf shouldErrorLog

    private infix fun String.logIf(b: Boolean) = if (b) println(this) else Unit

    public enum class Level {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        ;

        public companion object {
            override fun toString(): String = entries.joinToString(", ")
        }
    }
}

public interface HasLogger {
    public val logger: Logger
}

public interface NoLogger : HasLogger {
    override val logger: Logger get() = noLogger
}

public val noLogger: Logger = object : Logger(logLevel = null) {}
