plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
}

group = "${libs.versions.group.id.get()}.compiler.emitters"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

val enableNative = (findProperty("wirespec.enableNative") as String?).toBoolean()

kotlin {
    if (enableNative) {
        macosX64()
        macosArm64()
        linuxX64()
        mingwX64()
    }
    js(IR) {
        nodejs()
        useEsModules()
    }
    jvm {
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }

    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.compiler.get()
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":src:compiler:core"))
                api(project(":src:compiler:ir"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.bundles.kotest)
                implementation(project(":src:compiler:test"))
            }
        }
    }
}

// ---------- Emitter fixtures: shared `.txt` expectations + codegen + updater ----------
//
// Tests reference `EmitterFixtures.<testName>` constants. Those constants are
// generated from the matching `<lang>.txt` files in :src:compiler:test/fixtures.
// Run `./gradlew :src:compiler:emitters:java:updateEmitterFixtures` to regenerate
// the .txt files by actually running the emitter against the shared fixture inputs.

val emitterLanguage = "java"
val emitterPackage = "community.flock.wirespec.emitters.java"
val emitterUpdaterMainClass = "$emitterPackage.UpdateEmitterFixturesKt"

val fixturesSourceDir = rootProject.layout.projectDirectory.dir("src/compiler/test/fixtures").asFile
val generatedSourcesDir = layout.buildDirectory.dir("generated/emitterFixtures/commonTest/kotlin")

val generateEmitterFixtures = tasks.register("generateEmitterFixtures") {
    description = "Generate Kotlin constants from $emitterLanguage fixture files."
    group = "build"

    // Capture locals so the action closure doesn't reference script-level state
    // (which the configuration cache can't serialize).
    val fixturesDir = fixturesSourceDir
    val outDirProvider = generatedSourcesDir
    val language = emitterLanguage
    val packageName = emitterPackage

    inputs.dir(fixturesDir).withPropertyName("fixtures").optional(true)
    inputs.property("language", language)
    inputs.property("package", packageName)
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
            val literal = escapeKotlinString(file.readText())
            sb.appendLine("    val ${dir.name}: String = \"$literal\"")
        }
        sb.appendLine("}")
        outFile.writeText(sb.toString())
    }
}

kotlin.sourceSets.named("commonTest") {
    kotlin.srcDir(generateEmitterFixtures)
}

tasks.register<JavaExec>("updateEmitterFixtures") {
    description = "Run the $emitterLanguage emitter and overwrite the shared fixture files."
    group = "verification"

    val jvmTest = tasks.named<Test>("jvmTest")
    classpath = files(jvmTest.map { it.classpath })
    mainClass.set(emitterUpdaterMainClass)
    args(fixturesSourceDir.absolutePath)

    dependsOn(jvmTest.map { it.dependsOn })
    outputs.upToDateWhen { false }
}
