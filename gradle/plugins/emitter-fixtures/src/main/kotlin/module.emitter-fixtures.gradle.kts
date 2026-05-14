import org.gradle.api.tasks.testing.Test

// Codegen + updater task for emitter test fixtures.
//
// Each emitter test asserts against `EmitterFixtures.<testName>` constants. Those
// constants are generated from .txt files in the shared :src:compiler:test/fixtures
// directory, keyed by test name + language.
//
// Configure via the `emitterFixtures { ... }` extension. The emitter's
// build.gradle.kts is also responsible for wiring the generated directory into
// the commonTest source set, e.g.:
//
//     kotlin.sourceSets.named("commonTest") {
//         kotlin.srcDir(tasks.named("generateEmitterFixtures"))
//     }
//
// Run `./gradlew :path:to:emitter:updateEmitterFixtures` to regenerate the .txt
// files by actually running the emitter against the shared fixture inputs.

val ext = extensions.create<EmitterFixturesExtension>("emitterFixtures")

val fixturesSourceDir = rootProject.layout.projectDirectory.dir("src/compiler/test/fixtures").asFile
val generatedSourcesDir = layout.buildDirectory.dir("generated/emitterFixtures/commonTest/kotlin")

tasks.register("generateEmitterFixtures") {
    description = "Generate Kotlin constants from emitter fixture files."
    group = "build"

    // Capture locals so the action closure doesn't reference script-level state
    // (which the configuration cache cannot serialize).
    val fixturesDir = fixturesSourceDir
    val outDirProvider = generatedSourcesDir
    val languageProvider = ext.language
    val packageProvider = ext.emitterPackage

    inputs.dir(fixturesDir).withPropertyName("fixtures").optional(true)
    inputs.property("language", languageProvider)
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

        val language = languageProvider.get()
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

        val dirs = fixturesDir.listFiles { f -> f.isDirectory }
            ?.sortedBy { it.name }
            .orEmpty()
        for (dir in dirs) {
            val file = dir.resolve("$language.txt")
            if (!file.exists()) continue
            val literal = escapeKotlinString(file.readText().replace("\r\n", "\n"))
            sb.appendLine("    val ${dir.name}: String = \"$literal\"")
        }
        sb.appendLine("}")
        outFile.writeText(sb.toString())
    }
}

tasks.register<JavaExec>("updateEmitterFixtures") {
    description = "Run the emitter and overwrite the shared fixture files for this language."
    group = "verification"

    val jvmTest = tasks.named<Test>("jvmTest")
    classpath = files(jvmTest.map { it.classpath })
    mainClass.set(ext.updaterMainClass)
    args(fixturesSourceDir.absolutePath)

    dependsOn(jvmTest.map { it.dependsOn })
    outputs.upToDateWhen { false }
}
