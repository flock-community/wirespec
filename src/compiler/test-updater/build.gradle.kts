plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "${libs.versions.group.id.get()}.compiler"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(project(":src:compiler:test"))
    implementation(project(":src:compiler:emitters:java"))
    implementation(project(":src:compiler:emitters:kotlin"))
    implementation(project(":src:compiler:emitters:typescript"))
    implementation(project(":src:compiler:emitters:python"))
    implementation(project(":src:compiler:emitters:wirespec"))
    implementation(libs.arrow.core)
}

application {
    mainClass.set("community.flock.wirespec.compiler.testupdater.MainKt")
}
