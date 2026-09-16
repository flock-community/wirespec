plugins {
    `kotlin-dsl`
}

dependencies {
    // The Kotlin Multiplatform plugin itself comes from the module applying this convention.
    compileOnly(libs.kotlin.gradle.plugin)
}
