import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.npm.NpmTask

group = "${Settings.groupId}.lsp.node.server"
version = Settings.version

buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("com.moowork.gradle:gradle-node-plugin:1.2.0")
    }
}

plugins {
    base
    id("com.moowork.node") version "1.3.1"
}

tasks.named<NpmTask>("npm_run_build") {
    inputs.files(fileTree("src"))
    inputs.file("package.json")
    inputs.file("package-lock.json")
    inputs.file("tsconfig.json")
    outputs.dir("build")
}

val packageNpmApp by tasks.registering(Jar::class) {
    dependsOn("npm_run_build")
    baseName = "npm-app"
    extension = "jar"
    destinationDir = file("${projectDir}/build_packageNpmApp")
    from("build") {
        // optional path under which output will be visible in Java classpath, e.g. static resources path
        into("static")
    }
}

val npmResources by configurations.creating

configurations.named("default").get().extendsFrom(npmResources)

// expose the artifact created by the packaging task
artifacts {
    add(npmResources.name, packageNpmApp.get().archivePath) {
        builtBy(packageNpmApp)
        type = "jar"
    }
}

tasks.assemble {
    dependsOn(packageNpmApp)
}

tasks.clean {
    delete(packageNpmApp.get().archivePath)
}