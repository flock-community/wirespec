# Gradle plugin

The gradle plugin can be use compile wirespec

## Usage

```kts
tasks.register<CompileWirespecTask>("wirespec-aigentic-kotlin") {
    input = layout.projectDirectory.dir("wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.custom"
    languages = listOf(Language.Kotlin)
}

tasks.register<ConvertWirespecTask>("wirespec-aigentic-openapi") {
    input = layout.projectDirectory.file("openapi/petstorev3.json")
    output = layout.buildDirectory.dir("generated")
    format = Format.OpenApiV3
    packageName = "community.flock.wirespec.openapi"
    languages = listOf(Language.Kotlin)
}

tasks.register<CustomWirespecTask>("wirespec-aigentic") {
    input = layout.projectDirectory.dir("wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.wirespec"
    emitter = KotlinSerializableEmitter::class.java
    shared = KotlinShared.source
    extension = "kt"
}
```