import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    baseline = rootProject.file("config/detekt/baseline.xml")
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
