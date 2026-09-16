import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// Kotlin/Native targets are opt-in per build: only those listed in `wirespec.nativeTargets`
// (comma-separated) are added, so a build compiles and links just the natives it needs.
//
//     ./gradlew -Pwirespec.nativeTargets=macosArm64,linuxX64 build
//
// Apply alongside the Kotlin Multiplatform plugin.

val nativeTargets = providers.gradleProperty("wirespec.nativeTargets").orNull.orEmpty()
    .split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)
    .toSet()

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        nativeTargets.forEach { target ->
            when (target) {
                "macosX64" -> macosX64()
                "macosArm64" -> macosArm64()
                "linuxX64" -> linuxX64()
                "mingwX64" -> mingwX64()
                else -> error("Unknown Kotlin/Native target '$target' in wirespec.nativeTargets, expected macosX64, macosArm64, linuxX64, or mingwX64")
            }
        }
    }
}
