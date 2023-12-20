import Versions.ARROW
import Versions.CLI
import Versions.KOTEST
import Versions.KOTEST_ARROW

object Versions {
    const val ARROW = "1.2.1"
    const val CLI = "0.3.6"
    const val INTELLIJ = "1.15.0"
    const val KOTEST = "5.7.2"
    const val KOTEST_ARROW = "1.4.0"
}

object Libraries {
    const val ARROW_CORE = "io.arrow-kt:arrow-core:$ARROW"
    const val CLI_LIB = "org.jetbrains.kotlinx:kotlinx-cli:$CLI"
    const val KOTEST_ENGINE = "io.kotest:kotest-framework-engine:$KOTEST"
    const val KOTEST_ASSERTIONS = "io.kotest:kotest-assertions-core:$KOTEST"
    const val KOTEST_ASSERTIONS_ARROW = "io.kotest.extensions:kotest-assertions-arrow:$KOTEST_ARROW"
}

object Plugins {
    val intellij = Plugin("org.jetbrains.intellij", Versions.INTELLIJ)
}

data class Plugin(val name: String, val version: String)
