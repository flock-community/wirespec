import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotest)
}

group = "${libs.versions.group.id.get()}.plugin.cli"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
}

kotlin {
    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    macosX64 { build() }
    macosArm64 { build() }
    linuxX64 { build() }
    js(IR) { build() }
    jvm {
        withJava()
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
                implementation(project(":src:plugin:arguments"))
                implementation(project(":src:converter:openapi"))
                implementation(libs.clikt)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.bundles.kotest)
            }
        }
        jsMain {
            dependencies {
                implementation(project(":src:compiler:lib"))
            }
        }
    }
}

tasks.withType<Jar> {
    doFirst {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "community.flock.wirespec.plugin.cli.MainKt")
        }
        val main by kotlin.jvm().compilations.getting
        val files = main.runtimeDependencyFiles.files
            .filter { it.name.endsWith("jar") }
            .map(::zipTree)
        from(files)
    }
}

fun KotlinNativeTargetWithHostTests.build() {
    binaries {
        executable {
            entryPoint = "community.flock.wirespec.plugin.cli.main"
        }
    }
}

fun KotlinJsTargetDsl.build() {
    nodejs()
    binaries.executable()
}
