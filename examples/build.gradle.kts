description = "Drives the builds of all Wirespec example projects"

// Example tasks deliberately avoid lifecycle names (build, check, clean) so the default build
// never touches them; they only run via the aggregates below or an explicit task path.
val gradleExamples = projectDir.listFiles().orEmpty()
    .filter { it.resolve("settings.gradle.kts").exists() }
    .sortedBy { it.name }

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

fun aggregate(name: String, taskDescription: String, moduleTask: String, nestedTasks: List<String>, excluded: List<String> = emptyList()) =
    gradleExamples.map { nestedGradleBuild(it, "${moduleTask.removeSuffix("Example")}-${it.name}", nestedTasks, excluded) }.let { nested ->
        tasks.register(name) {
            group = "examples"
            description = taskDescription
            dependsOn(subprojects.map { project -> project.tasks.matching { it.name == moduleTask } })
            dependsOn(nested)
        }
    }

aggregate("buildExamples", "Build and test all example projects", "buildExample", listOf("check"))
aggregate("cleanExamples", "Clean all example projects", "cleanExample", listOf("clean"))
aggregate("formatExamples", "Format all example projects", "formatExample", listOf("spotlessApply"))
aggregate("yoloExamples", "Build all Maven and Gradle example projects without running tests", "yoloExample", listOf("check"), listOf("test"))
tasks.register("installWrappers") {
    group = "examples"
    description = "Install the sbt wrapper and the cargo toolchain where missing"
    dependsOn(subprojects.map { project -> project.tasks.matching { it.name == "installWrapper" } })
    dependsOn(installCargo)
}

subprojects {
    fun execExample(name: String, vararg command: String) = tasks.register<Exec>(name) {
        group = "examples"
        workingDir = projectDir
        commandLine(*command)
    }

    when {
        projectDir.resolve("pom.xml").exists() -> {
            execExample("buildExample", "../mvnw", "verify")
            execExample("cleanExample", "../mvnw", "clean")
            execExample("yoloExample", "../mvnw", "verify", "-DskipTests")
            if (projectDir.resolve("pom.xml").readText().contains("<id>format</id>")) {
                execExample("formatExample", "../mvnw", "test-compile", "-Pformat")
            }
        }

        projectDir.resolve("package.json").exists() -> {
            val install = execExample("npmInstall", "npm", "ci")
            execExample("buildExample", "npm", "run", "build").configure { dependsOn(install) }
            execExample("cleanExample", "npm", "run", "clean")
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
            cargoExample("cleanExample", "clean")
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
            execExample("cleanExample", "./sbt", "clean").configure { dependsOn(installWrapper) }
        }
    }
}
