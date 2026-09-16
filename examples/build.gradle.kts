import org.gradle.kotlin.dsl.support.serviceOf

description = "Drives the builds of all Wirespec example projects"

// Example tasks deliberately avoid lifecycle names (build, check, clean) so the default build
// never touches them; they only run via the aggregates below or an explicit task path.
val gradleExamples = projectDir.listFiles().orEmpty()
    .filter { it.resolve("settings.gradle.kts").exists() }
    .sortedBy { it.name }

// The Maven wrapper scripts are never checked in: they are downloaded fresh from Maven Central,
// at the version pinned in .mvn/wrapper/maven-wrapper.properties, and shared by all Maven examples.
val mavenWrapperVersion = projectDir.resolve(".mvn/wrapper/maven-wrapper.properties").readLines()
    .single { it.startsWith("wrapperVersion=") }.substringAfter('=')

val mavenWrapper = configurations.create("mavenWrapper")

dependencies {
    mavenWrapper("org.apache.maven.wrapper:maven-wrapper-distribution:$mavenWrapperVersion:only-script@zip")
}

val mvnw = layout.buildDirectory.file("maven-wrapper/mvnw").get().asFile.path

val installMavenWrapper = tasks.register<Sync>("installMavenWrapper") {
    group = "examples"
    description = "Download the Maven wrapper fresh from Maven Central"
    val archives = serviceOf<ArchiveOperations>()
    from(mavenWrapper.elements.map { zips -> zips.map { archives.zipTree(it.asFile) } })
    // mvnw locates its wrapper configuration next to the script itself
    from(layout.projectDirectory.dir(".mvn")) { into(".mvn") }
    into(layout.buildDirectory.dir("maven-wrapper"))
    filePermissions { unix("rwxr-xr-x") }
}

val graalvmAvailable = System.getenv("GRAALVM_HOME") != null ||
    System.getenv("PATH").orEmpty().split(File.pathSeparator).any { File(it, "native-image").canExecute() }

val cargoBin = File(System.getProperty("user.home"), ".cargo/bin")

val installCargo = tasks.register<Exec>("installCargo") {
    group = "examples"
    description = "Install the cargo toolchain via rustup when missing"
    val cargoBinPath = cargoBin.path
    onlyIf {
        (System.getenv("PATH").orEmpty().split(File.pathSeparator) + cargoBinPath)
            .none { File(it, "cargo").canExecute() }
    }
    commandLine("bash", "-c", "curl -fsSL https://sh.rustup.rs | sh -s -- -y --profile minimal")
}

// The standalone Gradle examples are driven through nested builds, run lazily with this build's
// Gradle version, so their mavenLocal-only plugins resolve when the task runs, not at configuration.
fun nestedGradleBuild(example: File, name: String, buildTasks: List<String>, excluded: List<String> = emptyList()) =
    tasks.register<GradleBuild>(name) {
        group = "examples"
        setDir(example)
        startParameter.setTaskNames(buildTasks)
        startParameter.setExcludedTaskNames(excluded)
    }

// Every example gets a uniform entry point :examples:<verb>-<name>, so callers (CI included) need
// no knowledge of an example's build tool.
fun aggregate(name: String, taskDescription: String, moduleTask: String, gradleExample: (File, String) -> TaskProvider<*>) =
    moduleTask.removeSuffix("Example").let { verb ->
        val delegates = subprojects.map { project ->
            tasks.register("$verb-${project.name}") {
                group = "examples"
                dependsOn(project.tasks.matching { it.name == moduleTask })
            }
        } + gradleExamples.map { gradleExample(it, "$verb-${it.name}") }
        tasks.register(name) {
            group = "examples"
            description = taskDescription
            dependsOn(delegates)
        }
    }

aggregate("buildExamples", "Build and test all example projects", "buildExample") { example, taskName ->
    nestedGradleBuild(example, taskName, listOf("check"))
}
aggregate("cleanExamples", "Clean all example projects", "cleanExample") { example, taskName ->
    tasks.register<Delete>(taskName) {
        group = "examples"
        delete(example.resolve("build"))
    }
}
aggregate("formatExamples", "Format all example projects", "formatExample") { example, taskName ->
    nestedGradleBuild(example, taskName, listOf("spotlessApply"))
}
aggregate("yoloExamples", "Build all Maven and Gradle example projects without running tests", "yoloExample") { example, taskName ->
    nestedGradleBuild(example, taskName, listOf("check"), listOf("test"))
}
tasks.register("installWrappers") {
    group = "examples"
    description = "Install the Maven and sbt wrappers and the cargo toolchain where missing"
    dependsOn(subprojects.map { project -> project.tasks.matching { it.name == "installWrapper" } })
    dependsOn(installMavenWrapper, installCargo)
}

subprojects {
    fun execExample(name: String, vararg command: String) = tasks.register<Exec>(name) {
        group = "examples"
        workingDir = projectDir
        commandLine(*command)
    }

    // Cleaning deletes an example's build output directly: starting Maven, npm, cargo or sbt
    // only to delete a directory is slow.
    fun deleteOutputs(outputs: List<String>) = tasks.register<Delete>("cleanExample") {
        group = "examples"
        delete(outputs.map { projectDir.resolve(it) })
    }

    when {
        projectDir.resolve("pom.xml").exists() -> {
            fun mvnExample(name: String, vararg args: String) =
                execExample(name, mvnw, *args).configure { dependsOn(installMavenWrapper) }
            val pom = projectDir.resolve("pom.xml").readText()
            // an example declaring the GraalVM plugin builds native when a native toolchain is present
            val native = pom.contains("native-maven-plugin") && graalvmAvailable
            mvnExample("buildExample", *(if (native) arrayOf("-Pnative") else emptyArray()), "verify")
            // what `mvn clean` removes: the target directory of the project and of each module
            deleteOutputs(listOf("target") + Regex("<module>(.+?)</module>").findAll(pom).map { "${it.groupValues[1]}/target" })
            mvnExample("yoloExample", "verify", "-DskipTests")
            if (pom.contains("<id>format</id>")) {
                mvnExample("formatExample", "test-compile", "-Pformat")
            }
        }

        projectDir.resolve("package.json").exists() -> {
            val install = execExample("npmInstall", "npm", "ci")
            execExample("buildExample", "npm", "run", "build").configure { dependsOn(install) }
            // `npm run clean` also removes node_modules, but that holds dependencies, not build output
            deleteOutputs(listOf("src/gen", "lib"))
            execExample("formatExample", "npm", "run", "format").configure { dependsOn(install) }
        }

        projectDir.resolve("Cargo.toml").exists() -> {
            fun cargoExample(name: String, vararg args: String) = execExample(name, "cargo", *args).also { task ->
                task.configure {
                    dependsOn(installCargo)
                    environment("PATH", "$cargoBin${File.pathSeparator}${System.getenv("PATH").orEmpty()}")
                }
            }
            val generate = execExample("generateExample", "bash", "gen.sh")
            val cargoBuild = cargoExample("cargoBuild", "build").also { it.configure { dependsOn(generate) } }
            val cargoTest = cargoExample("cargoTest", "test").also { it.configure { dependsOn(cargoBuild) } }
            tasks.register("buildExample") {
                group = "examples"
                dependsOn(cargoTest)
            }
            deleteOutputs(listOf("target"))
        }

        projectDir.resolve("build.sbt").exists() -> {
            val sbt = projectDir.resolve("sbt")
            val installWrapper = tasks.register<Exec>("installWrapper") {
                group = "examples"
                description = "Install the sbt wrapper (sbt-extras) into ${project.name}"
                onlyIf { !sbt.exists() }
                workingDir = projectDir
                commandLine(
                    "bash", "-c",
                    "curl -fsSL -o sbt https://raw.githubusercontent.com/dwijnand/sbt-extras/master/sbt && chmod +x sbt",
                )
            }
            execExample("buildExample", "./sbt", "compile", "test").configure { dependsOn(installWrapper) }
            deleteOutputs(listOf("target"))
        }
    }
}
