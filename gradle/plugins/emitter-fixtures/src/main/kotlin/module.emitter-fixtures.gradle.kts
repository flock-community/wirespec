import org.gradle.api.tasks.testing.Test

// Codegen + updater wiring for emitter test fixtures.
//
// Each emitter test asserts against `EmitterFixtures.<testName>` constants. Those
// constants are generated from .txt files in the per-emitter `fixtures/` directory,
// one file per test. The actual runtime that overwrites the .txt files lives in
// the `:src:compiler:test` `EmitterFixtureUpdater` object; each emitter has a
// tiny `UpdateEmitterFixtures.kt` main that delegates to it.
//
// Apply alongside the Kotlin Multiplatform plugin and configure via:
//
//     emitterFixtures {
//         emitterPackage = "community.flock.wirespec.emitters.java"
//     }
//
// The emitter's build.gradle.kts wires the generated source dir into commonTest:
//
//     kotlin.sourceSets.named("commonTest") {
//         kotlin.srcDir(tasks.named("generateEmitterFixtures"))
//     }

val ext = extensions.create<EmitterFixturesExtension>("emitterFixtures")

val fixturesSourceDir = layout.projectDirectory.dir("fixtures").asFile
val generatedSourcesDir = layout.buildDirectory.dir("generated/emitterFixtures/commonTest/kotlin")

tasks.register("generateEmitterFixtures") {
    description = "Generate Kotlin constants from emitter fixture files."
    group = "build"

    // Capture locals so the action closure doesn't reference script-level state
    // (which the configuration cache cannot serialize).
    val fixturesDir = fixturesSourceDir
    val outDirProvider = generatedSourcesDir
    val packageProvider = ext.emitterPackage

    inputs.dir(fixturesDir).withPropertyName("fixtures").optional(true)
    inputs.property("package", packageProvider)
    outputs.dir(outDirProvider).withPropertyName("generatedSources")

    doLast {
        fun escapeKotlinString(s: String): String = buildString(s.length + 16) {
            for (ch in s) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '$' -> append("\\\$")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (ch.code < 0x20) {
                        append("\\u")
                        append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        append(ch)
                    }
                }
            }
        }

        val packageName = packageProvider.get()
        val outFile = outDirProvider.get().asFile
            .resolve(packageName.replace('.', '/'))
            .resolve("EmitterFixtures.kt")
        outFile.parentFile.mkdirs()

        val sb = StringBuilder()
        sb.appendLine("// AUTO-GENERATED — do not edit. Run `:updateEmitterFixtures` to refresh.")
        sb.appendLine("@file:Suppress(\"ktlint\", \"MaxLineLength\")")
        sb.appendLine("package $packageName")
        sb.appendLine()
        sb.appendLine("internal object EmitterFixtures {")

        val files = fixturesDir.listFiles { f -> f.isFile && f.extension == "txt" }
            ?.sortedBy { it.name }
            .orEmpty()
        for (file in files) {
            val literal = escapeKotlinString(file.readText().replace("\r\n", "\n"))
            sb.appendLine("    val ${file.nameWithoutExtension}: String = \"$literal\"")
        }
        sb.appendLine("}")
        outFile.writeText(sb.toString())
    }
}

tasks.register<JavaExec>("updateEmitterFixtures") {
    description = "Run the emitter and overwrite the fixture files for this module."
    group = "verification"

    // Resolve the test runtime classpath without dragging `jvmTest` itself into
    // the task graph — the whole point of this task is to refresh fixtures when
    // they're out of sync, so we can't gate on tests passing. `files(provider {})`
    // (vs. `files(jvmTest.map { ... })`) hides the source task from Gradle, so
    // only the FileCollection's own build deps (compile tasks) are inherited.
    classpath = files(provider { tasks.named<Test>("jvmTest").get().classpath })
    mainClass.set(ext.emitterPackage.map { "$it.UpdateEmitterFixturesKt" })
    args(fixturesSourceDir.absolutePath)

    dependsOn(tasks.named("jvmTestClasses"))
    outputs.upToDateWhen { false }
}
