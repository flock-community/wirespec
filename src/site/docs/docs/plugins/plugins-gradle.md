---
title: Gradle
slug: /plugins/gradle
sidebar_position: 4
---

# Wirespec Gradle Plugin

![Maven Central](https://img.shields.io/maven-central/v/community.flock.wirespec.plugin.maven/wirespec-maven-plugin)

This document describes how to use the Wirespec Gradle plugin to integrate Wirespec compilation into your Gradle build process.  The plugin allows you to automatically generate code from your Wirespec definitions during your build.

## Installation

To use the Wirespec Gradle plugin, you need to add it to your `build.gradle.kts` file. Here's how:

1.  **Add the Plugin:**

```kts
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import community.flock.wirespec.plugin.gradle.ConvertWirespecTask
import community.flock.wirespec.plugin.gradle.CustomWirespecTask

plugins {
    id("community.flock.wirespec.plugin.gradle") version "{{WIRESPEC_VERSION}}"
}

tasks.register<CompileWirespecTask>("wirespec-typescript") {
    description = "Compile Wirespec to TypeScript"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.generated.typescript"
    languages = listOf(Language.TypeScript)
}

tasks.register<ConvertWirespecTask>("wirespec-openapi") {
    description = "Convert Wirespec to OpenAPISpec"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("openapi")
    format = "OpenAPI"
}

tasks.register<CustomWirespecTask>("wirespec-kotlin") {
    description = "Compile Wirespec to Kotlin"
    group = "Wirespec compile"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.generated.kotlin"
    emitter = KotlinSerializableEmitter::class.java
    sharedPackage = KotlinShared.packageString
    sharedSource = KotlinShared.source
    extension = FileExtension.Kotlin.value
}

class KotlinSerializableEmitter : KotlinEmitter("community.flock.wirespec.generated.kotlin", noLogger) {

    override fun emit(type: Type, ast: AST): String = """
        |@kotlinx.serialization.Serializable
        |${super.emit(type, ast)}
    """.trimMargin()

    override fun emit(refined: Refined): String = """
        |@kotlinx.serialization.Serializable
        |${super.emit(refined)}
    """.trimMargin()
}

```
