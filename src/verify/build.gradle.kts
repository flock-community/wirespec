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

tasks.test {
    useJUnitPlatform()
    systemProperty("buildDir", layout.buildDirectory.get().asFile.absolutePath)
}

dependencies {
    implementation(project(":src:compiler:core"))
    implementation(project(":src:compiler:test"))
    implementation(project(":src:compiler:emitters:java"))
    implementation(project(":src:compiler:emitters:kotlin"))
    implementation(project(":src:compiler:emitters:python"))
    implementation(project(":src:compiler:emitters:typescript"))
    implementation(libs.bundles.kotest)
    implementation(libs.testcontainers)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.params)
}
