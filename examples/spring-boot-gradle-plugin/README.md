# Example: How to use the Wirespec Gradle Plugin
## Wirespec Gradle Plugin Configuration
```gradle
wirespec {
    input = "$projectDir/src/main/wirespec"
    kotlin {
        output = "$buildDir/generated/wirespec"
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
