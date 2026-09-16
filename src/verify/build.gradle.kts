plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "${libs.versions.group.id.get()}.verify"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

val verifyEnabled = providers.gradleProperty("verify").isPresent
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("buildDir", layout.buildDirectory.get().asFile.absolutePath)
    enabled = verifyEnabled
}

// `allTests` gives every language its own test task, and those run in parallel: only the tests of a
// single language share a container and workspace, so only they have to run one after another.
// VerifyUtil.kt fails when this list and its `languages` differ.
val languages = listOf("java-17", "java-21", "kotlin-1", "kotlin-2", "python", "typescript", "rust", "scala")

val languageTests = languages.map { language ->
    tasks.register<Test>("test-$language") {
        group = "verification"
        description = "Runs the verify tests for $language"
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        systemProperty("verify.language", language)
        systemProperty("verify.languages", languages.joinToString(","))
    }
}

tasks.register("allTests") {
    group = "verification"
    description = "Runs the verify tests of all languages in parallel"
    dependsOn(languageTests)
}

dependencies {
    implementation(project(":src:compiler:core"))
    implementation(project(":src:compiler:test"))
    implementation(project(":src:compiler:emitters:java"))
    implementation(project(":src:compiler:emitters:kotlin"))
    implementation(project(":src:compiler:emitters:python"))
    implementation(project(":src:compiler:emitters:typescript"))
    implementation(project(":src:compiler:emitters:rust"))
    implementation(project(":src:compiler:emitters:scala"))
    implementation(libs.bundles.kotest)
    implementation(libs.testcontainers)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotest.runner.junit5)
}
