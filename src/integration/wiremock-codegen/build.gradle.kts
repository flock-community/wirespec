import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.fromVersion(libs.versions.kotlin.api.get()))
        languageVersion.set(KotlinVersion.fromVersion(libs.versions.kotlin.language.get()))
    }
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(project(":src:compiler:core"))
    implementation(project(":src:compiler:emitters:java"))
    implementation(project(":src:compiler:emitters:kotlin"))
}
