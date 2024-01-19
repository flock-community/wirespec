# Example: How to use the Wirespec Gradle Plugin

## Wirespec Gradle Plugin Configuration

```gradle
wirespec {
    input = "$projectDir/src/main/wirespec"
    kotlin {
        packageName = "community.flock.wirespec.generated"
        output = "$buildDir/generated/community/flock/wirespec/generated"
    }
    typescript {
        output = "$projectDir/src/main/typescript/generated"
    }
}

tasks.build {
    dependsOn("wirespec")
}
```

According to the [actual build.gradle.kts](build.gradle.kts) file.
