plugins {
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spotless)
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
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
        kotlin {
            target("**/*.kt", "**/*.kts")
            targetExclude("**/tmp/**", "**/generated/**", "**/build/**", "**/resources/**", "**/*Emitter.kt")
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
            repositories {
                maven {
                    name = "oss"
                    val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                    url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                    credentials {
                        username = System.getenv("SONATYPE_USERNAME")
                        password = System.getenv("SONATYPE_PASSWORD")
                    }
                }
            }
            withType<MavenPublication> {

                // Stub javadoc.jar artifact
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

    // We're using kotlinx.io in tests as well. For (node)js test, test-resources aren't readily available.
    // With this additional config of adding two copy tasks, and setting an explicit dependency on the
    // test tasks ensures the test-resources are readily available within every sub-project
    //
    // https://github.com/Kotlin/kotlinx-io/issues/265
    // https://gist.github.com/dellisd/a1df42787d42b41cd3ce16f573984674
    afterEvaluate {
        val copyTestResourcesForJs by tasks.registering(Copy::class) {
            group = "nodejs"
            description = "Copy js specific test-resources for nodejs test task (located at src/*Test/resources)"

            logger.info("Copying test resources for ${project.path}")
            val projectFullName = project.path.replace(Project.PATH_SEPARATOR, "-")
            val buildDir = rootProject.layout.buildDirectory.get()
            from("$projectDir/src")
            include("*Test/resources")
            into("$buildDir/js/packages/${rootProject.name}$projectFullName-test/src")
        }

        if (project.tasks.findByName("jsNodeTest") != null) {
            project.tasks.named("jsNodeTest").configure {
                dependsOn(copyTestResourcesForJs)
            }
        }
    }
}

