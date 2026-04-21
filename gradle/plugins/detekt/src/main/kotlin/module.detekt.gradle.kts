import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    id("io.gitlab.arturbosch.detekt")
}

// detekt 1.23.x embeds Kotlin 2.0.x and refuses to run when a newer Kotlin is
// present on its runtime classpath. Pin anything Kotlin-related on the detekt
// configurations to 2.0.21 so the analyzer stays on its own stdlib.
configurations.matching {
    it.name == "detekt" || it.name.startsWith("detektPlugins")
}.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.0.21")
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    baseline = file("detekt-baseline.xml")
    ignoreFailures = false
    parallel = true
    source.setFrom(
        "src/main/kotlin",
        "src/commonMain/kotlin",
        "src/jvmMain/kotlin",
        "src/jsMain/kotlin",
        "src/nativeMain/kotlin",
        "src/linuxX64Main/kotlin",
        "src/mingwX64Main/kotlin",
        "src/macosX64Main/kotlin",
        "src/macosArm64Main/kotlin",
    )
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
    exclude("**/build/**", "**/generated/**", "**/generated-sources/**", "**/resources/**")
}

// The Kotlin Multiplatform integration registers per-target Detekt tasks that run
// with type resolution; those require the detekt-embedded Kotlin version (2.0.x)
// to match the project's Kotlin compiler, which it does not here. Disable them so
// only the top-level `detekt` task (no type resolution) runs.
tasks.matching { task ->
    (task is Detekt || task is DetektCreateBaselineTask) &&
        task.name != "detekt" && task.name != "detektBaseline"
}.configureEach {
    enabled = false
}
