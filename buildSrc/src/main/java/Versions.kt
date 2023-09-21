import Versions.ARROW
import Versions.COROUTINES

object Versions {
    const val ARROW = "1.2.1"
    const val COROUTINES = "1.6.4"
    const val INTELLIJ = "1.15.0"
}

object Libraries {
    const val ARROW_CORE = "io.arrow-kt:arrow-core:$ARROW"
    const val KOTLIN_COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINES"
    const val KOTLIN_COROUTINES_NATIVE = "org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$COROUTINES"
    const val KOTLIN_COROUTINES_JVM = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$COROUTINES"
}

object Plugins {
    val intellij = Plugin("org.jetbrains.intellij", Versions.INTELLIJ)
}

data class Plugin(val name: String, val version: String)
