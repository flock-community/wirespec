# Gradle plugin

The gradle plugin can be used to compile wirespec

## Usage

```kts
tasks.register<CompileWirespecTask>("wirespec-kotlin") {
    input = layout.projectDirectory.dir("wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.custom"
    languages = listOf(Language.Kotlin)
}

tasks.register<ConvertWirespecTask>("wirespec-openapi") {
    input = layout.projectDirectory.file("openapi/petstorev3.json")
    output = layout.buildDirectory.dir("generated")
    format = Format.OpenAPIV3
    packageName = "community.flock.wirespec.openapi"
    languages = listOf(Language.Kotlin)
}

tasks.register<CustomWirespecTask>("wirespec-custom") {
    input = layout.projectDirectory.dir("wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.wirespec"
    emitter = KotlinSerializableEmitter::class.java
    shared = KotlinShared.source
    extension = "kt"
}
```
