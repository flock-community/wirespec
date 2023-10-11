import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.8.10"
}

val dokkaOutputDir = "$buildDir/dokka"

val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(dokkaOutputDir)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaOutputDir)
}

repositories {
    mavenCentral()
}

allprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")

    tasks.getByName<DokkaTask>("dokkaHtml") {
        outputDirectory.set(file(dokkaOutputDir))
    }

    signing {
        setRequired{System.getenv("VERSION") != null}
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
                artifact(javadocJar)
                pom {
                    name.set("Wirespec")
                    description.set("Type safe wires made easy")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/MIT")
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

