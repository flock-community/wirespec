import Versions.ARROW
import Versions.COROUTINES
import Versions.KOTEST
import Versions.KOTEST_ARROW

object Versions {
    const val ARROW = "1.2.1"
    const val COROUTINES = "1.6.4"
    const val INTELLIJ = "1.15.0"
    const val KOTEST = "5.7.2"
    const val KOTEST_ARROW = "1.4.0"
}

object Libraries {
    const val ARROW_CORE = "io.arrow-kt:arrow-core:$ARROW"
    const val KOTLIN_COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINES"
    const val KOTLIN_COROUTINES_NATIVE = "org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$COROUTINES"
    const val KOTLIN_COROUTINES_JVM = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$COROUTINES"
    const val KOTEST_ASSERTIONS = "io.kotest:kotest-assertions-core:$KOTEST"
    const val KOTEST_ASSERTIONS_ARROW = "io.kotest.extensions:kotest-assertions-arrow:$KOTEST_ARROW"
}

object Plugins {
    val intellij = Plugin("org.jetbrains.intellij", Versions.INTELLIJ)
}

data class Plugin(val name: String, val version: String)
