task<Exec>("build") {
    dependsOn(":plugin:gradle:publishToMavenLocal")
    commandLine("./gradlew", "--build-file", "app.gradle.kts", "wirespec", "build")
}