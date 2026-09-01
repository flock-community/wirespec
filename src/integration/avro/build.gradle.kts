plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
    maven(uri("https://packages.confluent.io/maven"))
}

val enableNative = (findProperty("wirespec.enableNative") as String?).toBoolean()

kotlin {
    // Kotlin 2.0 is this module's consumer-compatibility floor, matching the
    // kotlin_libraries floor: the current compiler rejects the repo-wide 1.9
    // floor for the JS/native/metadata compilations this module now has, and a
    // JVM-only 1.9 floor would sit below commonMain's, which KGP forbids.
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
    if (enableNative) {
        macosX64()
        macosArm64()
        linuxX64()
        mingwX64()
    }
    js(IR) {
        nodejs()
    }
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                // AvroExtension/Utils build the language-neutral IR and read the
                // Avro schema model; the actual Avro runtime (kafka.avro) is only
                // referenced by fully-qualified name in generated source, so it is
                // not a compile dependency here.
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:ir"))
                implementation(project(":src:converter:avro"))
                implementation(libs.kotlinx.serialization)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:ir"))
                implementation(project(":src:compiler:emitters:java"))
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:integration:wirespec"))
                implementation(libs.kafka.avro)
                implementation(libs.spring.boot.test)
                implementation(libs.kotlin.junit)
            }
        }
    }
}
