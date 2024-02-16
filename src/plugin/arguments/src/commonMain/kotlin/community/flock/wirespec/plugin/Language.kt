package community.flock.wirespec.plugin

sealed interface Language {
    enum class Jvm : Language { Java, Kotlin, Scala }
    enum class Script : Language { TypeScript }
    enum class Spec : Language { Wirespec }

    companion object {
        fun toMap() = values().associateBy { it.name }.mapValues { (_, v) -> v as Language }
        override fun toString() = values().joinToString()

        private fun values(): List<Enum<*>> = Jvm.entries + Script.entries + Spec.entries
    }
}

enum class FileExtension(val ext: String) {
    Java("java"), Kotlin("kt"), Scala("scala"), TypeScript("ts"), Wirespec("ws"), Json("json")
}
