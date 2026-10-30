plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    id("module.wirespec-codegen")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

wirespecCodegen {
    basePackage = "community.flock.wirespec.integration.spring"
    sources = layout.projectDirectory.dir("src/jvmTest/resources")
}

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.api.get()))
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.language.get()))
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
        val mainCompilation = compilations.getByName("main")
        compilations.named("codegen") {
            defaultSourceSet.dependencies {
                implementation(project(":src:compiler:emitters:java"))
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:converter:openapi"))
            }
        }
        // Spring Boot 4 scenario: runs main, compiled against Spring Boot 3 / Spring Framework 6,
        // on a consumer's Spring Boot 4 / Spring Framework 7 / Jackson 3 classpath. Covers what the
        // regular suite cannot: the Jackson 3 conditional wiring (Jackson 2 and 3 share
        // jackson-annotations) and binary compatibility with Spring Framework 7.
        compilations.create("springBoot4Test") {
            associateWith(mainCompilation)
            defaultSourceSet {
                kotlin.srcDir(wirespecCodegen.outputDir.dir("kotlin"))
                dependencies {
                    implementation(project.dependencies.platform(libs.spring.boot4.dependencies))
                    implementation(project(":src:integration:wirespec"))
                    implementation(project(":src:integration:jackson"))
                    implementation(libs.bundles.jackson3)
                    implementation(libs.spring.boot.web)
                    implementation(libs.spring.boot.test)
                    implementation(libs.kotlin.test)
                    implementation(libs.kotlin.junit)
                    implementation(libs.wiremock)
                    runtimeOnly(libs.junit.launcher)
                }
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                // The Spring extensions only reshape the language-neutral IR with
                // fully-qualified raw annotations/source, so they need the
                // compiler/IR API at compile time but no Spring runtime — that
                // comes from the consumer's build.
                compileOnly(project(":src:compiler:core"))
                compileOnly(project(":src:compiler:emitters:kotlin"))
                compileOnly(project(":src:compiler:emitters:java"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.bundles.kotest)
                implementation(project(":src:integration:wirespec"))
                implementation(project(":src:compiler:test"))
                implementation(project(":src:compiler:core"))
            }
        }
        jvmMain {
            dependencies {
                compileOnly(project(":src:compiler:core"))
                compileOnly(project(":src:compiler:emitters:kotlin"))
                compileOnly(project(":src:compiler:emitters:java"))
                api(project(":src:integration:wirespec"))
                implementation(project(":src:integration:jackson"))
                // Both Jackson versions are compileOnly: the consumer's Spring Boot
                // (3 → Jackson 2, 4 → Jackson 3) supplies the one actually used. The
                // Wirespec serializer is selected conditionally at runtime, v3 preferred.
                compileOnly(libs.bundles.jackson2)
                compileOnly(libs.bundles.jackson3)
                // Pinned to the Kotlin backwards-compatibility floor: kotlin-reflect
                // ships metadata at its own version, and the 2.3.x artifact cannot be
                // read by a Kotlin 2.0 compiler. Declaring the 2.0 floor keeps Spring
                // consumers on Kotlin 2.0+ buildable while still resolving upward for
                // consumers already on a newer Kotlin.
                implementation(libs.kotlin.reflect.compat)
                implementation(libs.kotlinx.coroutines.reactor)
                implementation(libs.spring.boot.web)
                implementation(libs.spring.webflux)
                runtimeOnly(libs.junit.launcher)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:converter:openapi"))
                implementation(project(":src:integration:wirespec"))
                implementation(libs.spring.boot.test)
                implementation(libs.bundles.jackson2)
                implementation(libs.kotlin.junit)
                implementation(libs.kotlinx.io.core)
                implementation(libs.wiremock)
            }
        }
    }
}

// The dependency-management plugin rewrites every requested version on every configuration to the
// Spring Boot 3 BOM, the constraints of the Spring Boot 4 platform included. Undo that on the
// springBoot4Test classpaths, so Gradle's own conflict resolution lets the platform's versions win.
configurations
    .matching { it.name.startsWith("jvmSpringBoot4Test") }
    .configureEach {
        resolutionStrategy.eachDependency {
            requested.version?.takeIf { it != target.version }?.let { useVersion(it) }
        }
    }

// The dependency-management plugin hooks into POM generation with a reference to the Project, which
// the configuration cache can't store. Publishing then runs without the cache instead of failing.
tasks.withType<GenerateMavenPom>().configureEach {
    notCompatibleWithConfigurationCache("io.spring.dependency-management's POM customization references the Project")
}

tasks.named<JavaCompile>("compileJvmSpringBoot4TestJava") {
    source(wirespecCodegen.outputDir.dir("java"))
    dependsOn("generateWirespecTestSources")
}

tasks.named("compileSpringBoot4TestKotlinJvm") {
    dependsOn("generateWirespecTestSources")
}

val jvmSpringBoot4TestCompilation = kotlin.jvm().compilations.named("springBoot4Test")

val jvmSpringBoot4Test = tasks.register<Test>("jvmSpringBoot4Test") {
    description = "Runs Spring integration tests on a Spring Boot 4 / Spring Framework 7 / Jackson 3 classpath."
    group = "verification"
    val compilation = jvmSpringBoot4TestCompilation.get()
    testClassesDirs = compilation.output.classesDirs
    classpath = compilation.output.allOutputs + compilation.runtimeDependencyFiles
    useJUnitPlatform()
}

// CI runs jvmTest, not check.
tasks.named("jvmTest") {
    dependsOn(jvmSpringBoot4Test)
}
