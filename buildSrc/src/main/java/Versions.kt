import Versions.arrow
import Versions.coroutines

object Versions {
    const val arrow = "1.1.4"
    const val coroutines = "1.6.4"
    const val intellij = "1.12.0"
}

object Libraries {
    const val `arrow-core` = "io.arrow-kt:arrow-core:$arrow"
    const val `kotlin-coroutines` = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines"
}

object Plugins {
    val intellij = Plugin("org.jetbrains.intellij", Versions.intellij)
}

data class Plugin(val name: String, val version: String)
