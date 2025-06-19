import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.`maven-publish`

plugins {
    `maven-publish`
    signing
}

publishing {
    // Configure all publications
    publications.withType<MavenPublication> {
        // Stub javadoc.jar artifact
        artifact(
            tasks.register("${name}JavadocJar", Jar::class) {
                archiveClassifier.set("javadoc")
                archiveAppendix.set(this@withType.name)
            },
        )

        // Provide artifacts information required by Maven Central
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

signing {
    setRequired { System.getenv("GPG_PRIVATE_KEY") != null }
    useInMemoryPgpKeys(
        System.getenv("GPG_PRIVATE_KEY"),
        System.getenv("GPG_PASSPHRASE"),
    )
    sign(publishing.publications)
}
