import com.github.dkorotych.gradle.maven.exec.MavenExec

plugins {
    id("com.github.dkorotych.gradle-maven-exec") version "3.0"
}

tasks.register<MavenExec>("build") {
    dependsOn(":plugin:maven-plugin:publishToMavenLocal")
    goals(listOf("clean", "package"))
}