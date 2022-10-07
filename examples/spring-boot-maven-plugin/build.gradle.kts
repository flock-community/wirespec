task<Exec>("build") {
    dependsOn(":plugin:maven:publishToMavenLocal")
    commandLine("./mvnw", "clean", "package")
}