task<Exec>("build") {
    dependsOn(":plugin:maven:publishToMavenLocal")
    commandLine("./gradlew", "--build-file", "app.gradle.kts", "wirespec", "build")
}