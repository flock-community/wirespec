plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.dokka) apply false
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

val dokka = libs.plugins.dokka.get().pluginId

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = dokka)

    signing {
        setRequired { System.getenv("GPG_PRIVATE_KEY") != null }
        useInMemoryPgpKeys(
            System.getenv("GPG_PRIVATE_KEY"),
            System.getenv("GPG_PASSPHRASE")
        )
        sign(publishing.publications)
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
}
