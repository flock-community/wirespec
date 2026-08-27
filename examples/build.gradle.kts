import java.io.File

description = "Drives the builds of all Wirespec example projects"

val examples = projectDir.listFiles().orEmpty().filter(File::isDirectory).sortedBy(File::getName)

fun File.taskSuffix() = name.split("-").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }

fun aggregate(name: String, taskDescription: String) = tasks.register(name) {
    group = "examples"
    description = taskDescription
}

val buildExamples = aggregate("build", "Build and test all example projects")
val cleanExamples = aggregate("clean", "Clean all example projects")
val formatExamples = aggregate("format", "Format all example projects")
val yoloExamples = aggregate("yolo", "Build all Maven and Gradle example projects without running tests")
val installWrappers = aggregate("installWrappers", "Install the Maven and Gradle wrappers into every example project")

fun exec(
    name: String,
    example: File,
    into: TaskProvider<Task>,
    after: TaskProvider<out Task>? = null,
    vararg command: String,
) = tasks.register<Exec>(name) {
    group = "examples"
    workingDir = example
    commandLine(*command)
    after?.let { dependsOn(it) }
}.also { task -> into.configure { dependsOn(task) } }

examples.filter { it.resolve("pom.xml").exists() }.forEach { example ->
    val suffix = example.taskSuffix()
    val wrapper = tasks.register<Copy>("installMavenWrapper$suffix") {
        group = "wrapper"
        description = "Install the Maven wrapper into ${example.name}"
        from(layout.projectDirectory.dir("maven/wrapper")) { include("mvnw", "mvnw.cmd") }
        from(layout.projectDirectory.dir("maven/wrapper")) {
            include("maven-wrapper.properties")
            into(".mvn/wrapper")
        }
        into(example)
    }.also { task -> installWrappers.configure { dependsOn(task) } }
    exec("build$suffix", example, buildExamples, wrapper, "./mvnw", "verify")
    exec("clean$suffix", example, cleanExamples, wrapper, "./mvnw", "clean")
    exec("yolo$suffix", example, yoloExamples, wrapper, "./mvnw", "verify", "-DskipTests")
    if (example.resolve("pom.xml").readText().contains("<id>format</id>")) {
        exec("format$suffix", example, formatExamples, wrapper, "./mvnw", "test-compile", "-Pformat")
    }
}

examples.filter { it.resolve("settings.gradle.kts").exists() }.forEach { example ->
    val suffix = example.taskSuffix()
    val wrapper = tasks.register<Copy>("installGradleWrapper$suffix") {
        group = "wrapper"
        description = "Install the Gradle wrapper into ${example.name}"
        from(layout.projectDirectory) { include("gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar") }
        from(layout.projectDirectory) {
            include("gradle/wrapper/gradle-wrapper.properties")
            // each example pins its own Gradle version; only seed the properties when absent
            eachFile { if (example.resolve(path).exists()) exclude() }
        }
        into(example)
    }.also { task -> installWrappers.configure { dependsOn(task) } }
    exec("build$suffix", example, buildExamples, wrapper, "./gradlew", "check")
    exec("clean$suffix", example, cleanExamples, wrapper, "./gradlew", "clean")
    exec("format$suffix", example, formatExamples, wrapper, "./gradlew", "spotlessApply")
    exec("yolo$suffix", example, yoloExamples, wrapper, "./gradlew", "check", "-x", "test")
}

examples.filter { it.resolve("package.json").exists() }.forEach { example ->
    val suffix = example.taskSuffix()
    val install = tasks.register<Exec>("npmInstall$suffix") {
        group = "examples"
        workingDir = example
        commandLine("npm", "ci")
    }
    exec("build$suffix", example, buildExamples, install, "npm", "run", "build")
    exec("clean$suffix", example, cleanExamples, null, "npm", "run", "clean")
    exec("format$suffix", example, formatExamples, install, "npm", "run", "format")
}

examples.filter { it.resolve("Cargo.toml").exists() }.forEach { example ->
    val suffix = example.taskSuffix()
    val generate = tasks.register<Exec>("generate$suffix") {
        group = "examples"
        workingDir = example
        commandLine("bash", "gen.sh")
    }
    exec("build$suffix", example, buildExamples, generate, "cargo", "build")
    exec("clean$suffix", example, cleanExamples, null, "cargo", "clean")
}

examples.filter { it.resolve("build.sbt").exists() }.forEach { example ->
    val suffix = example.taskSuffix()
    exec("build$suffix", example, buildExamples, null, "sbt", "compile")
    exec("clean$suffix", example, cleanExamples, null, "sbt", "clean")
}
