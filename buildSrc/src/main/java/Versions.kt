import Versions.ARROW
import Versions.CLI
import Versions.KOTEST
import Versions.KOTEST_ARROW
import Versions.KOTLIN

object Versions {
    const val KOTLIN = "1.9.24"
    const val KOTLIN_COMPILER = "1.9"
    const val ARROW = "1.2.1"
    const val CLI = "4.2.1"
    const val KOTEST = "5.7.2"
    const val KOTEST_ARROW = "1.4.0"
}

object Libraries {
    const val KOTLIN_STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN"
    const val KOTLIN_REFLECT = "org.jetbrains.kotlin:kotlin-reflect:$KOTLIN"
    const val ARROW_CORE = "io.arrow-kt:arrow-core:$ARROW"
    const val CLI_LIB = "com.github.ajalt.clikt:clikt:$CLI"
    const val KOTEST_ENGINE = "io.kotest:kotest-framework-engine:$KOTEST"
    const val KOTEST_ASSERTIONS = "io.kotest:kotest-assertions-core:$KOTEST"
    const val KOTEST_ASSERTIONS_ARROW = "io.kotest.extensions:kotest-assertions-arrow:$KOTEST_ARROW"
}
