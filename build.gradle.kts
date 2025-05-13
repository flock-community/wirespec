plugins {
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spotless)
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0" apply false
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("SONATYPE_USERNAME"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
        }
    }
}

val dokkaId = libs.plugins.dokka.get().pluginId
val spotlessId = libs.plugins.spotless.get().pluginId

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = dokkaId)
    apply(plugin = spotlessId)

    signing {
        setRequired { System.getenv("GPG_PRIVATE_KEY") != null }
        useInMemoryPgpKeys(
            System.getenv("GPG_PRIVATE_KEY"),
            System.getenv("GPG_PASSPHRASE")
        )
        sign(publishing.publications)
    }

    spotless {
        val exclude = listOf(
            "**/.github/**",
            "**/.gradle/**",
            "**/.idea/**",
            "**/.intellijPlatform/**",
            "**/.kotlin/**",
            "**/build/**",
            "**/vscode/**",
            "**/docs/**",
            "**/playground/**",
            "**/tmp/**",
            "**/generated/**",
            "**/resources/**",
            "**/node_modules/**",
            "**/*.lock",
            "**/*Emitter.kt",
        ).toTypedArray()

        format("misc") {
            target("**/*.md")
            targetExclude(*exclude)
            endWithNewline()
        }

        format("wirespec") {
            target("**/*.ws")
            targetExclude(*exclude)
            endWithNewline()
        }

        kotlin {
            target("**/*.kt", "**/*.kts")
            targetExclude(*exclude, "**/*Emitter.kt",)
            ktlint().editorConfigOverride(
                mapOf("ktlint_code_style" to "intellij_idea"),
            )
            suppressLintsFor {
                step = "ktlint"
                shortCode = "standard:enum-entry-name-case"
            }
        }
    }


    publishing {
        publications {
            withType<MavenPublication> {

                artifact(tasks.register("${name}JavadocJar", Jar::class) {
                    archiveClassifier.set("javadoc")
                    archiveAppendix.set(this@withType.name)
                })

                pom {
                    name.set("Wirespec")
                    description.set("Type safe wires made easy")
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://opensource.org/license/apache-2-0")
                        }
                    }
                    url.set("https://flock.community")
                    issueManagement {
                        system.set("Github")
                        url.set("https://github.com/flock-community/wirespec/issues")
                    }
                    scm {
                        connection.set("https://github.com/flock-community/wirespec.git")
                        url.set("https://github.com/flock-community/wirespec")
                    }
                    developers {
                        developer {
                            name.set("Jerre van Veluw")
                            email.set("jerre.van.veluw@flock.community")
                        }
                        developer {
                            name.set("Willem Veelenturf")
                            email.set("willem.veelenturf@flock.community")
                        }
                    }
                }
            }
        }
    }

    afterEvaluate {
        val copyTestResourcesForJs by tasks.registering(Copy::class) {
            group = "nodejs"
            description = "Copy js specific test-resources for nodejs test task (located at src/*Test/resources)"

            logger.info("Copying test resources for ${project.path}")
            val projectFullName = project.path.replace(Project.PATH_SEPARATOR, "-")
            val buildDir = rootProject.layout.buildDirectory.get()
            from("$projectDir/src")
            include("*Test/resources/**/*")
            into("$buildDir/js/packages/${rootProject.name}$projectFullName-test/src")
        }

        if (project.tasks.findByName("jsNodeTest") != null) {
            project.tasks.named("jsNodeTest").configure {
                dependsOn(copyTestResourcesForJs)
            }
        }
    }
}
