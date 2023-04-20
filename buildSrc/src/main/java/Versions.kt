import Versions.arrow
import Versions.coroutines

object Versions {
    const val arrow = "1.1.4"
    const val coroutines = "1.6.4"
    const val intellij = "1.13.0"
}

object Libraries {
    const val arrow_core = "io.arrow-kt:arrow-core:$arrow"
    const val kotlin_coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines"
    const val kotlin_coroutines_native = "org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$coroutines"
    const val kotlin_coroutines_jvm = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutines"
}

object Plugins {
    val intellij = Plugin("org.jetbrains.intellij", Versions.intellij)
}

data class Plugin(val name: String, val version: String)
